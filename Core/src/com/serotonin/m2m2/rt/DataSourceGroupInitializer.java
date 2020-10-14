/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 *
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.module.DataSourceDefinition.StartPriority;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;

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

    private final List<DataSourceVO> dataSources;
    private final boolean useMetrics;
    private final StartPriority startPriority;
    private final ExecutorService executor;

    /**
     *
     * @param group
     * @param logMetrics
     * @param threadPoolSize
     */
    public DataSourceGroupInitializer(StartPriority startPriority, List<DataSourceVO> dataSources, boolean logMetrics,
                                      ExecutorService executor) {
        this.startPriority = startPriority;
        this.dataSources = dataSources;
        this.useMetrics = logMetrics;
        this.executor = executor;
    }

    public List<DataSourceVO> initialize() {
        long startTs = Common.timer.currentTimeMillis();
        if (useMetrics && log.isInfoEnabled()) {
            log.info("Initializing " + dataSources.size() + " " + startPriority + " priority data sources");
        }
        List<DataSourceVO> initialized = this.initializeImpl();
        if (useMetrics && log.isInfoEnabled()) {
            log.info("Initialization of " + dataSources.size() + " " + startPriority +
                    " priority data sources took " + (Common.timer.currentTimeMillis() - startTs) + "ms");
        }
        return initialized;
    }

    /**
     * Blocking method that will attempt to start all datasources in parallel using threadPoolSize number of threads at most.
     * @return List of all data sources that need to begin polling.
     */
    public List<DataSourceVO> initializeImpl() {
        List<Future<DataSourceVO>> results = new ArrayList<>();
        List<DataSourceVO> initialized = new ArrayList<>();

        //Add and Start the tasks
        for (DataSourceVO dataSource : dataSources) {
            Future<DataSourceVO> future = executor.submit(() -> {
                if (Common.runtimeManager.initializeDataSourceStartup(dataSource)) {
                    return dataSource;
                } else {
                    throw new RuntimeException("Failed to initialize " + dataSource.getXid());
                }
            });
            results.add(future);
        }

        //Wait here until all threads are finished
        for (Future<DataSourceVO> future : results) {
            try {
                initialized.add(future.get());
            } catch (ExecutionException | CancellationException | InterruptedException e) {
                log.error("Failed to initialize some data sources", e);
            }
        }

        return initialized;
    }
}
