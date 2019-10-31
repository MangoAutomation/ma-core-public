/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.components;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.infiniteautomation.mango.monitor.LongMonitor;
import com.infiniteautomation.mango.monitor.ValueMonitorOwner;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.ModuleRegistry;

/**
 * @author Terry Packer
 *
 */
@Service
public class DiskUsageMonitoringService implements ValueMonitorOwner {
    
    private static final Log LOG = LogFactory.getLog(DiskUsageMonitoringService.class);
    public static final String MA_HOME_SIZE = "internal.monitor.MA_HOME_SIZE";
    public static final String FILE_STORE_SIZE_PREFIX = "internal.monitor.FILE_STORE.";

    private final ScheduledExecutorService scheduledExecutor;
    private final long period;
    private final LongMonitor maHomeSize = new LongMonitor(MA_HOME_SIZE, new TranslatableMessage(MA_HOME_SIZE), this);
    private final Map<String, LongMonitor> fileStoreMonitors;
    private volatile ScheduledFuture<?> scheduledFuture;

    @Autowired
    private DiskUsageMonitoringService(ScheduledExecutorService scheduledExecutor, @Value("${internal.monitor.diskUsage.pollPeriod:120000}") long period) {
        this.scheduledExecutor = scheduledExecutor;
        this.period = period;

        Common.MONITORED_VALUES.addIfMissingStatMonitor(maHomeSize);
        this.fileStoreMonitors = new HashMap<>();
        //Setup all filestores
        ModuleRegistry.getFileStoreDefinitions().values().stream().forEach( fs-> {
            LongMonitor monitor = new LongMonitor(FILE_STORE_SIZE_PREFIX + fs.getStoreName(), new TranslatableMessage("internal.monitor.fileStoreSize", fs.getStoreName()), this);
            this.fileStoreMonitors.put(fs.getStoreName(), monitor);
            Common.MONITORED_VALUES.addIfMissingStatMonitor(monitor);
        });
    }

    @PostConstruct
    private void postConstruct() {
        this.scheduledFuture = scheduledExecutor.scheduleAtFixedRate(this::doPoll, 0, this.period, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    private void preDestroy() {
        ScheduledFuture<?> scheduledFuture = this.scheduledFuture;
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
        }
    }

    private void doPoll() {
        maHomeSize.setValue(getSize(Common.MA_HOME_PATH.toFile()));
        fileStoreMonitors.entrySet().stream().forEach(monitor -> {
            monitor.getValue().setValue(getSize(ModuleRegistry.getFileStoreDefinition(monitor.getKey()).getRoot()));    
        });
    }

    @Override
    public void reset(String monitorId) {
        // nop
    }

    /**
     * Get the size of a directory
     * @return
     */
    private long getSize(File directory) {
        if(!directory.exists()) {
            return -1l;
        }
        try {
            return FileUtils.sizeOfDirectory(directory);
        }catch(Exception e) {
            LOG.error("Unable to compute size of " + directory.getAbsolutePath(), e);
            return 0l;
        }
    }
    
}
