/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt;

import java.util.List;
import java.util.concurrent.ExecutorService;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.module.DataSourceDefinition.StartPriority;
import com.serotonin.m2m2.rt.dataSource.DataSourceRT;

/**
 * This class is used at shutdown to terminate a group of data sources in parallel.
 * The group is generally a list of all data sources with the same priority level.
 *
 * @author Terry Packer
 * @author Jared Wiltshire
 *
 */
public class DataSourceGroupTerminator extends GroupProcessor<DataSourceRT<?>, Void> {
    private final StartPriority startPriority;

    public DataSourceGroupTerminator(ExecutorService executor, int maxConcurrency, StartPriority startPriority) {
        super(executor, maxConcurrency);
        this.startPriority = startPriority;
    }

    @Override
    public List<Void> process(List<DataSourceRT<?>> items) {
        long startTs = 0L;
        if (log.isInfoEnabled()) {
            startTs = Common.timer.currentTimeMillis();
            log.info("Terminating {} {} priority data sources in {} threads",
                    items.size(), startPriority, maxConcurrency);
        }
        List<Void> result = super.process(items);
        if (log.isInfoEnabled()) {
            log.info("Termination of {} {} priority data sources in {} threads took {} ms",
                    items.size(), startPriority, maxConcurrency, Common.timer.currentTimeMillis() - startTs);
        }
        return result;
    }

    @Override
    protected Void processItem(DataSourceRT<?> dataSourceRT, int itemId) {
        Common.runtimeManager.stopDataSourceShutdown(dataSourceRT.getId());
        return null;
    }
}
