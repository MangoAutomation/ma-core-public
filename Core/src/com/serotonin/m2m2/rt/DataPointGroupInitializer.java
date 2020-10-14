/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.serotonin.m2m2.rt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataPoint.DataPointWithEventDetectors;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.event.detector.AbstractPointEventDetectorVO;

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
    private final boolean useMetrics;
    private final PointValueDao dao;
    private final int groupSize;
    private final ExecutorService executor;

    /**
     * @param ds
     * @param dsPoints
     * @param dao
     * @param logMetrics
     * @param threadPoolSize
     */
    public DataPointGroupInitializer(DataSourceVO ds, List<DataPointWithEventDetectors> dsPoints, PointValueDao dao,
                                     boolean logMetrics, int groupSize, ExecutorService executor) {
        this.ds = ds;
        this.dsPoints = dsPoints;
        this.useMetrics = logMetrics;
        this.dao = dao;
        this.groupSize = groupSize;
        this.executor = executor;
    }

    public void initialize() {
        long startTs = Common.timer.currentTimeMillis();
        if (useMetrics && log.isInfoEnabled()) {
            log.info("Initializing " + dsPoints.size() + " data points");
        }
        this.initializeImpl();
        if (useMetrics && log.isInfoEnabled()) {
            log.info("Initialization of " + dsPoints.size() + " data points took " + (Common.timer.currentTimeMillis() - startTs) + "ms");
        }
    }

    /**
     * Blocking method that will attempt to start all data points in parallel using threadPoolSize number of threads at most.
     */
    public void initializeImpl() {
        List<Future<?>> results = new ArrayList<>();

        //Add and Start the tasks
        int numPoints = dsPoints.size();
        for (int from = 0; from < numPoints; from += groupSize) {
            int to = Math.min(from + groupSize, numPoints);
            DataPointSubGroupInitializer currentSubgroup = new DataPointSubGroupInitializer(dsPoints.subList(from, to));
            results.add(executor.submit(currentSubgroup));
        }

        //Wait here until all threads are finished
        for (Future<?> future : results) {
            try {
                future.get();
            } catch (ExecutionException | CancellationException | InterruptedException e) {
                log.error("Failed to start some data points", e);
            }
        }
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
    private class DataPointSubGroupInitializer implements Callable<Void> {

        private final Log log = LogFactory.getLog(DataPointSubGroupInitializer.class);
        private final List<DataPointWithEventDetectors> subgroup;

        public DataPointSubGroupInitializer(List<DataPointWithEventDetectors> subgroup) {
            this.subgroup = subgroup;
        }

        @Override
        public Void call() {
            if (useMetrics && log.isInfoEnabled()) {
                log.info(String.format("Initializing group of %d data points",
                        subgroup.size()));
            }

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
                log.warn("Failed to get latest point values for datasource " + ds.getXid() +
                        ". Mango will try to retrieve latest point values per point which will take longer.", e);
            }

            //Now start them
            int failedCount = 0;
            for (DataPointWithEventDetectors dataPoint : subgroup) {
                try {
                    // can be null, gets passed to com.serotonin.m2m2.rt.dataImage.PointValueCache.PointValueCache
                    List<PointValueTime> cache = latestValuesMap.get(dataPoint.getDataPoint().getId());
                    DataPointWithEventDetectorsAndCache config = new DataPointWithEventDetectorsAndCache(dataPoint, cache);
                    Common.runtimeManager.startDataPointStartup(config);
                } catch (Exception e) {
                    //Ensure only 1 can fail at a time
                    failedCount++;
                    log.error("Failed to start data point", e);
                }
            }

            if (useMetrics && log.isInfoEnabled()) {
                log.info(String.format("Successfully initialized %d of %d data points in group",
                        subgroup.size() - failedCount, subgroup.size()));
            }
            return null;
        }
    }
}
