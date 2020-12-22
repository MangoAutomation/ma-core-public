/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.serotonin.m2m2.rt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataPoint.DataPointWithEventDetectors;

/**
 * This class is used at startup to initialize data points on a single source in parallel.
 *
 * @author Terry Packer
 * @author Jared Wiltshire
 */
public class DataPointGroupInitializer extends GroupProcessor<List<DataPointWithEventDetectors>, Void> {

    private final PointValueDao dao;

    public DataPointGroupInitializer(ExecutorService executor, int maxConcurrency, PointValueDao dao) {
        super(executor, maxConcurrency);
        this.dao = dao;
    }

    public void initialize(List<DataPointWithEventDetectors> items, int groupSize) {
        long startTs = 0L;
        if (log.isInfoEnabled()) {
            startTs = Common.timer.currentTimeMillis();
            log.info("Initializing {} data points in {} threads",
                    items.size(), maxConcurrency);
        }

        // break into subgroups
        List<List<DataPointWithEventDetectors>> subgroups = new ArrayList<>();
        int numPoints = items.size();
        for (int from = 0; from < numPoints; from += groupSize) {
            int to = Math.min(from + groupSize, numPoints);
            subgroups.add(items.subList(from, to));
        }
        process(subgroups);

        if (log.isInfoEnabled()) {
            log.info("Initialization of {} data points in {} threads took {} ms",
                    items.size(), maxConcurrency, Common.timer.currentTimeMillis() - startTs);
        }
    }

    @Override
    protected Void processItem(List<DataPointWithEventDetectors> subgroup, int itemId) {
        long startTs = 0L;
        if (log.isInfoEnabled()) {
            startTs = Common.timer.currentTimeMillis();
            log.info("Initializing group {} of {} data points",
                    itemId,
                    subgroup.size());
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

        boolean failed = false;
        Map<Integer, List<PointValueTime>> latestValuesMap = new HashMap<>(subgroup.size());
        try {
            dao.getLatestPointValues(queryPoints, Long.MAX_VALUE, true, maxCacheSize,
                    (pvt, i) -> latestValuesMap.computeIfAbsent(pvt.getSeriesId(), (k) -> new ArrayList<>()).add(pvt));
        } catch (Exception e) {
            failed = true;
            log.warn("Failed to get latest point values for multiple points at once. " +
                    "Mango will fall back to retrieving latest point values per point which will take longer.", e);
        }

        //Now start them
        int failedCount = 0;
        for (DataPointWithEventDetectors dataPoint : subgroup) {
            try {
                // should only be submitted as null if we failed, gets passed to com.serotonin.m2m2.rt.dataImage.PointValueCache.PointValueCache
                // if we didn't fail then we can assume there is no value in the database
                List<PointValueTime> cache = null;
                if(!failed) {
                    cache = latestValuesMap.get(dataPoint.getDataPoint().getId());
                    if(cache == null) {
                        cache = Collections.emptyList();
                    }
                }
                DataPointWithEventDetectorsAndCache config = new DataPointWithEventDetectorsAndCache(dataPoint, cache);
                Common.runtimeManager.startDataPointStartup(config);
            } catch (Exception e) {
                //Ensure only 1 can fail at a time
                failedCount++;
                log.error("Failed to start data point", e);
            }
        }

        if (log.isInfoEnabled()) {
            log.info("Group {} successfully initialized {} of {} data points in {} ms",
                    itemId, subgroup.size() - failedCount, subgroup.size(), Common.timer.currentTimeMillis() - startTs);
        }
        return null;
    }

}
