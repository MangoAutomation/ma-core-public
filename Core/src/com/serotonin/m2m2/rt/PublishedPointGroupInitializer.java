/*
 * Copyright (C) 2021 RadixIot LLC. All rights reserved.
 */

package com.serotonin.m2m2.rt;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;

public class PublishedPointGroupInitializer extends GroupProcessor<List<PublishedPointVO>, Void> {

    public PublishedPointGroupInitializer(ExecutorService executor, int maxConcurrency) {
        super(executor, maxConcurrency);
    }

    public void initialize(List<PublishedPointVO> items, int groupSize) {
        long startTs = 0L;
        if (log.isInfoEnabled()) {
            startTs = Common.timer.currentTimeMillis();
            log.info("Initializing {} published points in {} threads",
                    items.size(), maxConcurrency);
        }

        // break into subgroups
        List<List<PublishedPointVO>> subgroups = new ArrayList<>();
        int numPoints = items.size();
        for (int from = 0; from < numPoints; from += groupSize) {
            int to = Math.min(from + groupSize, numPoints);
            subgroups.add(items.subList(from, to));
        }
        process(subgroups);

        if (log.isInfoEnabled()) {
            log.info("Initialization of {} published points in {} threads took {} ms",
                    items.size(), maxConcurrency, Common.timer.currentTimeMillis() - startTs);
        }
    }

    @Override
    protected Void processItem(List<PublishedPointVO> subgroup, int itemId) throws Exception {
        long startTs = 0L;
        if (log.isInfoEnabled()) {
            startTs = Common.timer.currentTimeMillis();
            log.info("Initializing group {} of {} published points",
                    itemId,
                    subgroup.size());
        }

        //Now start them
        int failedCount = 0;
        for (PublishedPointVO point : subgroup) {
            try {
                Common.runtimeManager.startPublishedPoint(point);
            } catch (Exception e) {
                //Ensure only 1 can fail at a time
                failedCount++;
                log.error("Failed to start published point", e);
            }
        }

        if (log.isInfoEnabled()) {
            log.info("Group {} successfully initialized {} of {} data points in {} ms",
                    itemId, subgroup.size() - failedCount, subgroup.size(), Common.timer.currentTimeMillis() - startTs);
        }
        return null;
    }
}
