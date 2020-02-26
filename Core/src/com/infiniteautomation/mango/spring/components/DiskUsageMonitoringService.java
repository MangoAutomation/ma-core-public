/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.components;

import java.io.File;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
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

import com.infiniteautomation.mango.monitor.MonitoredValues;
import com.infiniteautomation.mango.monitor.ValueMonitor;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.NoSQLProxy;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.FileStoreDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;

/**
 * Monitor partition sizes and optionally file stores and ma_home directories
 *
 * @author Terry Packer
 *
 */
@Service
public class DiskUsageMonitoringService {

    private final Log log = LogFactory.getLog(DiskUsageMonitoringService.class);

    public static final String MA_HOME_PARTITION_TOTAL_SPACE = "internal.monitor.MA_HOME_PARTITION_TOTAL_SPACE";
    public static final String MA_HOME_PARTITION_USABLE_SPACE = "internal.monitor.MA_HOME_PARTITION_USABLE_SPACE";
    public static final String MA_HOME_PARTITION_USED_SPACE = "internal.monitor.MA_HOME_PARTITION_USED_SPACE";

    public static final String NOSQL_PARTITION_TOTAL_SPACE = "internal.monitor.NOSQL_PARTITION_TOTAL_SPACE";
    public static final String NOSQL_PARTITION_USABLE_SPACE = "internal.monitor.NOSQL_PARTITION_USABLE_SPACE";
    public static final String NOSQL_PARTITION_USED_SPACE = "internal.monitor.NOSQL_PARTITION_USED_SPACE";

    public static final String SQL_PARTITION_TOTAL_SPACE = "internal.monitor.SQL_PARTITION_TOTAL_SPACE";
    public static final String SQL_PARTITION_USABLE_SPACE = "internal.monitor.SQL_PARTITION_USABLE_SPACE";
    public static final String SQL_PARTITION_USED_SPACE = "internal.monitor.SQL_PARTITION_USED_SPACE";

    public static final String FILESTORE_PARTITION_TOTAL_SPACE = "internal.monitor.FILESTORE_PARTITION_TOTAL_SPACE";
    public static final String FILESTORE_PARTITION_USABLE_SPACE = "internal.monitor.FILESTORE_PARTITION_USABLE_SPACE";
    public static final String FILESTORE_PARTITION_USED_SPACE = "internal.monitor.FILESTORE_PARTITION_USED_SPACE";

    public static final String MA_HOME_SIZE = "internal.monitor.MA_HOME_SIZE";
    public static final String FILE_STORE_SIZE_PREFIX = "internal.monitor.FILE_STORE.";

    private final ExecutorService executor;
    private final ScheduledExecutorService scheduledExecutor;
    private final long period;

    private final ValueMonitor<Long> maHomePartitionTotalSpace;
    private final ValueMonitor<Long> maHomePartitionUsableSpace;
    private final ValueMonitor<Long> maHomePartitionUsedSpace;
    private final ValueMonitor<Long> noSqlPartitionTotalSpace;
    private final ValueMonitor<Long> noSqlPartitionUsableSpace;
    private final ValueMonitor<Long> noSqlPartitionUsedSpace;
    private final ValueMonitor<Long> sqlPartitionTotalSpace;
    private final ValueMonitor<Long> sqlPartitionUsableSpace;
    private final ValueMonitor<Long> sqlPartitionUsedSpace;
    private final ValueMonitor<Long> filestorePartitionTotalSpace;
    private final ValueMonitor<Long> filestorePartitionUsableSpace;
    private final ValueMonitor<Long> filestorePartitionUsedSpace;
    private final ValueMonitor<Long> maHomeSize;

    private final Map<String, ValueMonitor<Long>> fileStoreMonitors = new HashMap<>();
    private volatile ScheduledFuture<?> scheduledFuture;

    /**
     *
     * @param scheduledExecutor
     * @param period - how often to recalculate the usage
     * @param monitorDirectories - monitor file stores and ma_home specifically (will require more CPU)
     */
    @Autowired
    private DiskUsageMonitoringService(ExecutorService executor,
            ScheduledExecutorService scheduledExecutor,
            @Value("${internal.monitor.diskUsage.pollPeriod:1800000}") long period,
            @Value("${internal.monitor.diskUsage.monitorDirectories:false}") boolean monitorDirectories,
            MonitoredValues monitoredValues) {

        this.executor = executor;
        this.scheduledExecutor = scheduledExecutor;
        this.period = period;

        maHomePartitionTotalSpace = monitoredValues.<Long>create(MA_HOME_PARTITION_TOTAL_SPACE)
                .name(new TranslatableMessage(MA_HOME_PARTITION_TOTAL_SPACE))
                .build();
        maHomePartitionUsableSpace = monitoredValues.<Long>create(MA_HOME_PARTITION_USABLE_SPACE)
                .name(new TranslatableMessage(MA_HOME_PARTITION_USABLE_SPACE))
                .build();
        maHomePartitionUsedSpace = monitoredValues.<Long>create(MA_HOME_PARTITION_USED_SPACE)
                .name(new TranslatableMessage(MA_HOME_PARTITION_USED_SPACE))
                .build();
        noSqlPartitionTotalSpace = monitoredValues.<Long>create(NOSQL_PARTITION_TOTAL_SPACE)
                .name(new TranslatableMessage(NOSQL_PARTITION_TOTAL_SPACE))
                .build();
        noSqlPartitionUsableSpace = monitoredValues.<Long>create(NOSQL_PARTITION_USABLE_SPACE)
                .name(new TranslatableMessage(NOSQL_PARTITION_USABLE_SPACE))
                .build();
        noSqlPartitionUsedSpace = monitoredValues.<Long>create(NOSQL_PARTITION_USED_SPACE)
                .name(new TranslatableMessage(NOSQL_PARTITION_USED_SPACE))
                .build();
        sqlPartitionTotalSpace = monitoredValues.<Long>create(SQL_PARTITION_TOTAL_SPACE)
                .name(new TranslatableMessage(SQL_PARTITION_TOTAL_SPACE))
                .build();
        sqlPartitionUsableSpace = monitoredValues.<Long>create(SQL_PARTITION_USABLE_SPACE)
                .name(new TranslatableMessage(SQL_PARTITION_USABLE_SPACE))
                .build();
        sqlPartitionUsedSpace = monitoredValues.<Long>create(SQL_PARTITION_USED_SPACE)
                .name(new TranslatableMessage(SQL_PARTITION_USED_SPACE))
                .build();
        filestorePartitionTotalSpace = monitoredValues.<Long>create(FILESTORE_PARTITION_TOTAL_SPACE)
                .name(new TranslatableMessage(FILESTORE_PARTITION_TOTAL_SPACE))
                .build();
        filestorePartitionUsableSpace = monitoredValues.<Long>create(FILESTORE_PARTITION_USABLE_SPACE)
                .name(new TranslatableMessage(FILESTORE_PARTITION_USABLE_SPACE))
                .build();
        filestorePartitionUsedSpace = monitoredValues.<Long>create(FILESTORE_PARTITION_USED_SPACE)
                .name(new TranslatableMessage(FILESTORE_PARTITION_USED_SPACE))
                .build();

        if (monitorDirectories) {
            maHomeSize = monitoredValues.<Long>create(MA_HOME_SIZE)
                    .name(new TranslatableMessage(MA_HOME_SIZE))
                    .build();

            //Setup all filestores
            ModuleRegistry.getFileStoreDefinitions().values().stream().forEach( fs-> {
                ValueMonitor<Long> monitor = monitoredValues.<Long>create(FILE_STORE_SIZE_PREFIX + fs.getStoreName())
                        .name(new TranslatableMessage("internal.monitor.fileStoreSize", fs.getStoreDescription()))
                        .build();
                this.fileStoreMonitors.put(fs.getStoreName(), monitor);
            });
        } else {
            maHomeSize = null;
        }
    }

    @PostConstruct
    private void postConstruct() {
        this.scheduledFuture = scheduledExecutor.scheduleAtFixedRate(() -> {
            executor.execute(this::doPoll);
        }, 0, this.period, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    private void preDestroy() {
        ScheduledFuture<?> scheduledFuture = this.scheduledFuture;
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
        }
    }

    private void doPoll() {
        //Volume information for MA Home Partition
        try {
            FileStore store = Files.getFileStore(Common.MA_HOME_PATH);
            maHomePartitionTotalSpace.setValue(store.getTotalSpace());
            maHomePartitionUsableSpace.setValue(store.getUsableSpace());
            maHomePartitionUsedSpace.setValue(store.getTotalSpace() - store.getUsableSpace());
        }catch(Exception e) {
            log.error("Unable to get MA_HOME partition usage", e);
        }
        //Volume information for NoSQL partition
        try {
            FileStore store = Files.getFileStore(Paths.get(NoSQLProxy.getDatabasePath()).toAbsolutePath());
            noSqlPartitionTotalSpace.setValue(store.getTotalSpace());
            noSqlPartitionUsableSpace.setValue(store.getUsableSpace());
            noSqlPartitionUsedSpace.setValue(store.getTotalSpace() - store.getUsableSpace());
        }catch(Exception e) {
            log.error("Unable to get NoSQL partition usage", e);
        }

        //Volume information for sql db partition (if possible)
        File dataDirectory = Common.databaseProxy.getDataDirectory();
        if(dataDirectory != null) {
            try{
                FileStore store = Files.getFileStore(dataDirectory.toPath());
                sqlPartitionTotalSpace.setValue(store.getTotalSpace());
                sqlPartitionUsableSpace.setValue(store.getUsableSpace());
                sqlPartitionUsedSpace.setValue(store.getTotalSpace() - store.getUsableSpace());
            }catch(Exception e) {
                log.error("Unable to get Filestore partition usage", e);
            }
        }

        //Volume information for filestore partition
        String location = Common.envProps.getString(FileStoreDefinition.FILE_STORE_LOCATION_ENV_PROPERTY);
        if (location == null || location.isEmpty()) {
            location = FileStoreDefinition.ROOT;
        }
        try{
            FileStore store = Files.getFileStore(Common.MA_HOME_PATH.resolve(location));
            filestorePartitionTotalSpace.setValue(store.getTotalSpace());
            filestorePartitionUsableSpace.setValue(store.getUsableSpace());
            filestorePartitionUsedSpace.setValue(store.getTotalSpace() - store.getUsableSpace());
        }catch(Exception e) {
            log.error("Unable to get Filestore partition usage", e);
        }

        if (this.maHomeSize != null) {
            maHomeSize.setValue(getSize(Common.MA_HOME_PATH.toFile()));
        }

        fileStoreMonitors.entrySet().stream().forEach(monitor -> {
            monitor.getValue().setValue(getSize(ModuleRegistry.getFileStoreDefinition(monitor.getKey()).getRoot()));
        });
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
            log.error("Unable to compute size of " + directory.getAbsolutePath(), e);
            return 0l;
        }
    }

}
