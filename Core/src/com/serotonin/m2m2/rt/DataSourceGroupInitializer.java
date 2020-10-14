/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 *
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.module.DataSourceDefinition.StartPriority;
import com.serotonin.m2m2.util.timeout.HighPriorityTask;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.timer.RejectedTaskReason;

/**
 * This class is used at startup to initialize data sources in parallel.
 *
 * The group is generally a list of all data sources with the same priority level.
 * The group is not initialized until all data sources have either started or failed to start.
 *
 * @author Terry Packer
 * @author Jared Wiltshire
 *
 */
public class DataSourceGroupInitializer {
    private final Log log = LogFactory.getLog(DataSourceGroupInitializer.class);

    private final List<DataSourceVO> group;
    private final int threadPoolSize;
    private final boolean useMetrics;
    private final StartPriority startPriority;

    /**
     *
     * @param group
     * @param logMetrics
     * @param threadPoolSize
     */
    public DataSourceGroupInitializer(StartPriority startPriority, List<DataSourceVO> group, boolean logMetrics, int threadPoolSize) {
        this.startPriority = startPriority;
        this.group = group;
        this.useMetrics = logMetrics;
        this.threadPoolSize = threadPoolSize;
    }

    /**
     * Blocking method that will attempt to start all datasources in parallel using threadPoolSize number of threads at most.
     * @return List of all data sources that need to begin polling.
     */
    public List<DataSourceVO> initialize() {
        long startTs = Common.timer.currentTimeMillis();
        int numDataSources = this.group.size();

        //Compute the size of the subGroup that each thread will use.
        int quotient = numDataSources / this.threadPoolSize;
        int remainder = numDataSources % this.threadPoolSize;
        int subGroupSize = remainder == 0 ? quotient : quotient + 1;

        if (useMetrics)
            log.info("Initializing " + this.group.size() + " " + this.startPriority.name() +
                    " priority data sources in " + this.threadPoolSize + " threads.");

        List<DataSourceSubGroupInitializer> groups = new ArrayList<>();
        List<DataSourceVO> polling = new ArrayList<>();

        //Add and Start the tasks
        for (int from = 0; from < numDataSources; from += subGroupSize) {
            int to = Math.min(from + subGroupSize, numDataSources);
            DataSourceSubGroupInitializer currentSubgroup = new DataSourceSubGroupInitializer(this.group.subList(from, to));
            Common.backgroundProcessing.execute(currentSubgroup);
            groups.add(currentSubgroup);
        }

        //Wait here until all threads are finished
        for (DataSourceSubGroupInitializer group : groups) {
            try {
                polling.addAll(group.future.get());
            } catch (ExecutionException | CancellationException | InterruptedException e) {
                log.error("Failed to start some data sources", e);
            }
        }

        if (this.useMetrics)
            log.info("Initialization of " + this.group.size() + " " + this.startPriority.name() +
                    " priority data sources took " + (Common.timer.currentTimeMillis() - startTs) + "ms");

        return polling;
    }

    /**
     * Initialize a sub group of the data sources in one thread.
     * @author Terry Packer
     * @author Jared Wiltshire
     */
    private static class DataSourceSubGroupInitializer extends HighPriorityTask {

        private final Log log = LogFactory.getLog(DataSourceSubGroupInitializer.class);
        private final List<DataSourceVO> subgroup;
        private final CompletableFuture<List<DataSourceVO>> future;

        public DataSourceSubGroupInitializer(List<DataSourceVO> subgroup) {
            super("Datasource subgroup initializer");
            this.subgroup = subgroup;
            this.future = new CompletableFuture<>();
        }

        @Override
        public void run(long runtime) {
            try {
                List<DataSourceVO> polling = new ArrayList<>();
                for (DataSourceVO config : subgroup) {
                    try {
                        if (Common.runtimeManager.initializeDataSourceStartup(config))
                            polling.add(config);
                    } catch (Exception e) {
                        //Ensure only 1 can fail at a time
                        log.error(e.getMessage(), e);
                    }
                }
                future.complete(polling);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        }

        @Override
        public boolean cancel() {
            future.cancel(false);
            return super.cancel();
        }

        @Override
        public void rejected(RejectedTaskReason reason) {
            future.cancel(false);
            super.rejected(reason);
        }
    }

}
