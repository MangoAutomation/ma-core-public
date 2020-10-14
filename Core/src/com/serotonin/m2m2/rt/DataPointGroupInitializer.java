/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.serotonin.m2m2.rt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.util.timeout.HighPriorityTask;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataPoint.DataPointWithEventDetectors;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.event.detector.AbstractPointEventDetectorVO;
import com.serotonin.timer.RejectedTaskReason;

/**
 * This class is used at startup to initialize data points on a single source in parallel.
 *
 * @author Terry Packer
 * @author Jared Wiltshire
 */
public class DataPointGroupInitializer {

    private final Log log = LogFactory.getLog(DataPointGroupInitializer.class);

    private final DataSourceVO ds;
    private final List<DataPointWithEventDetectors> dsPoints;
    private final int threadPoolSize;
    private final boolean useMetrics;
    private final PointValueDao dao;
    private final int minPointsPerThread;

    /**
     * @param ds
     * @param dsPoints
     * @param dao
     * @param logMetrics
     * @param threadPoolSize
     */
    public DataPointGroupInitializer(DataSourceVO ds, List<DataPointWithEventDetectors> dsPoints, PointValueDao dao,
                                     boolean logMetrics, int threadPoolSize, int minPointsPerThread) {
        this.ds = ds;
        this.dsPoints = dsPoints;
        this.useMetrics = logMetrics;
        this.threadPoolSize = threadPoolSize;
        this.dao = dao;
        this.minPointsPerThread = minPointsPerThread;
    }

    /**
     * Blocking method that will attempt to start all data points in parallel using threadPoolSize number of threads at most.
     */
    public void initialize() {
        long startTs = Common.timer.currentTimeMillis();
        int numPoints = this.dsPoints.size();

        //Compute the size of the subGroup that each thread will use.
        int quotient = numPoints / this.threadPoolSize;
        int remainder = numPoints % this.threadPoolSize;
        int subGroupSize = Math.max(remainder == 0 ? quotient : quotient + 1, minPointsPerThread);

        if (useMetrics)
            log.info("Initializing " + numPoints + " data points in up to " + this.threadPoolSize + " threads.");

        List<DataPointSubGroupInitializer> groups = new ArrayList<>();

        //Add and Start the tasks
        for (int from = 0; from < numPoints; from += subGroupSize) {
            int to = Math.min(from + subGroupSize, numPoints);
            DataPointSubGroupInitializer currentSubgroup = new DataPointSubGroupInitializer(this.dsPoints.subList(from, to));
            Common.backgroundProcessing.execute(currentSubgroup);
            groups.add(currentSubgroup);
        }

        for (DataPointSubGroupInitializer group : groups) {
            try {
                group.future.get();
            } catch (ExecutionException | CancellationException | InterruptedException e) {
                log.error("Failed to start some data points", e);
            }
        }

        if (this.useMetrics)
            log.info("Initialization of " + this.dsPoints.size() + " data points took " + (Common.timer.currentTimeMillis() - startTs) + "ms");
    }

    public static class DataPointWithEventDetectorsAndCache extends DataPointWithEventDetectors {
        private final List<PointValueTime> initialCache;

        public DataPointWithEventDetectorsAndCache(DataPointWithEventDetectors vo, List<PointValueTime> initialCache) {
            super(vo.getDataPoint(), vo.getEventDetectors());
            this.initialCache = initialCache;
        }

        public DataPointWithEventDetectorsAndCache(DataPointVO vo,
                                                   List<AbstractPointEventDetectorVO> detectors, List<PointValueTime> initialCache) {
            super(vo, detectors);
            this.initialCache = initialCache;
        }

        public List<PointValueTime> getInitialCache() {
            return initialCache;
        }
    }

    /**
     * Initialize a sub group of the data points in one thread.
     *
     * @author Terry Packer
     * @author Jared Wiltshire
     */
    private class DataPointSubGroupInitializer extends HighPriorityTask {

        private final Log log = LogFactory.getLog(DataPointSubGroupInitializer.class);
        private final List<DataPointWithEventDetectors> subgroup;
        private final CompletableFuture<Void> future;

        public DataPointSubGroupInitializer(List<DataPointWithEventDetectors> subgroup) {
            super("Data point subgroup initializer");
            this.subgroup = subgroup;
            this.future = new CompletableFuture<>();
        }

        @Override
        public void run(long runtime) {
            try {
                //Bulk request the latest values
                List<DataPointVO> queryPoints = subgroup.stream()
                        .map(DataPointWithEventDetectors::getDataPoint)
                        .collect(Collectors.toList());

                // Find the maximum cache size for all point in the datasource
                // This number of values will be retrieved for all points in the datasource
                // If even one point has a high cache size this *may* cause issues
                int maxCacheSize = queryPoints.stream()
                        .map(DataPointVO::getDefaultCacheSize)
                        .mapToInt(Integer::intValue)
                        .max()
                        .orElse(0);

                Map<Integer, List<PointValueTime>> latestValuesMap = new HashMap<>(subgroup.size());
                try {
                    dao.getLatestPointValues(queryPoints, Long.MAX_VALUE, true, maxCacheSize,
                            (pvt, i) -> latestValuesMap.computeIfAbsent(pvt.getId(), (k) -> new ArrayList<>()).add(pvt));
                } catch (Exception e) {
                    log.error("Failed to get latest point values for datasource " + ds.getXid() +
                            ". Mango will try to retrieve latest point values per point which will take longer.", e);
                }

                //Now start them
                for (DataPointWithEventDetectors dataPoint : subgroup) {
                    try {
                        // can be null, gets passed to com.serotonin.m2m2.rt.dataImage.PointValueCache.PointValueCache
                        List<PointValueTime> cache = latestValuesMap.get(dataPoint.getDataPoint().getId());
                        DataPointWithEventDetectorsAndCache config = new DataPointWithEventDetectorsAndCache(dataPoint, cache);
                        Common.runtimeManager.startDataPointStartup(config);
                    } catch (Exception e) {
                        //Ensure only 1 can fail at a time
                        log.error(e.getMessage(), e);
                    }
                }
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            } finally {
                if (useMetrics) {
                    log.info("Started " + subgroup.size() + " data points out of " + dsPoints.size());
                }
            }
        }

        @Override
        public boolean cancel() {
            this.future.cancel(false);
            return super.cancel();
        }

        @Override
        public void rejected(RejectedTaskReason reason) {
            this.future.cancel(false);
            super.rejected(reason);
        }
    }
}
