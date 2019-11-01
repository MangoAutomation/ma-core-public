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
import com.serotonin.m2m2.db.NoSQLProxy;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.FileStoreDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;

/**
 * @author Terry Packer
 *
 */
@Service
public class DiskUsageMonitoringService implements ValueMonitorOwner {
    
    private static final Log LOG = LogFactory.getLog(DiskUsageMonitoringService.class);
    
    public static final String MA_HOME_PARTITION_TOTAL_SPACE = "internal.monitor.MA_HOME_PARTITION_TOTAL_SPACE";
    public static final String MA_HOME_PARTITION_USABLE_SPACE = "internal.monitor.MA_HOME_PARTITION_USABLE_SPACE";

    public static final String NOSQL_PARTITION_TOTAL_SPACE = "internal.monitor.NOSQL_PARTITION_TOTAL_SPACE";
    public static final String NOSQL_PARTITION_USABLE_SPACE = "internal.monitor.NOSQL_PARTITION_USABLE_SPACE";
    
    public static final String SQL_PARTITION_TOTAL_SPACE = "internal.monitor.SQL_PARTITION_TOTAL_SPACE";
    public static final String SQL_PARTITION_USABLE_SPACE = "internal.monitor.SQL_PARTITION_USABLE_SPACE";

    public static final String FILESTORE_PARTITION_TOTAL_SPACE = "internal.monitor.FILESTORE_PARTITION_TOTAL_SPACE";
    public static final String FILESTORE_PARTITION_USABLE_SPACE = "internal.monitor.FILESTORE_PARTITION_USABLE_SPACE";

    
    public static final String MA_HOME_SIZE = "internal.monitor.MA_HOME_SIZE";
    public static final String FILE_STORE_SIZE_PREFIX = "internal.monitor.FILE_STORE.";

    private final ScheduledExecutorService scheduledExecutor;
    private final long period;
    
    private final LongMonitor maHomePartitionTotalSpace  = new LongMonitor(MA_HOME_PARTITION_TOTAL_SPACE, new TranslatableMessage(MA_HOME_PARTITION_TOTAL_SPACE), this);
    private final LongMonitor maHomePartitionUsableSpace  = new LongMonitor(MA_HOME_PARTITION_USABLE_SPACE, new TranslatableMessage(MA_HOME_PARTITION_USABLE_SPACE), this);

    private final LongMonitor noSqlPartitionTotalSpace  = new LongMonitor(NOSQL_PARTITION_TOTAL_SPACE, new TranslatableMessage(NOSQL_PARTITION_TOTAL_SPACE), this);
    private final LongMonitor noSqlPartitionUsableSpace  = new LongMonitor(NOSQL_PARTITION_USABLE_SPACE, new TranslatableMessage(NOSQL_PARTITION_USABLE_SPACE), this);
    
    private final LongMonitor sqlPartitionTotalSpace  = new LongMonitor(SQL_PARTITION_TOTAL_SPACE, new TranslatableMessage(SQL_PARTITION_TOTAL_SPACE), this);
    private final LongMonitor sqlPartitionUsableSpace  = new LongMonitor(SQL_PARTITION_USABLE_SPACE, new TranslatableMessage(SQL_PARTITION_USABLE_SPACE), this);

    private final LongMonitor filestorePartitionTotalSpace  = new LongMonitor(FILESTORE_PARTITION_TOTAL_SPACE, new TranslatableMessage(FILESTORE_PARTITION_TOTAL_SPACE), this);
    private final LongMonitor filestorePartitionUsableSpace  = new LongMonitor(FILESTORE_PARTITION_USABLE_SPACE, new TranslatableMessage(FILESTORE_PARTITION_USABLE_SPACE), this);
    
    private final LongMonitor maHomeSize = new LongMonitor(MA_HOME_SIZE, new TranslatableMessage(MA_HOME_SIZE), this);
    private final Map<String, LongMonitor> fileStoreMonitors;
    private volatile ScheduledFuture<?> scheduledFuture;

    @Autowired
    private DiskUsageMonitoringService(ScheduledExecutorService scheduledExecutor, @Value("${internal.monitor.diskUsage.pollPeriod:120000}") long period) {
        this.scheduledExecutor = scheduledExecutor;
        this.period = period;

        Common.MONITORED_VALUES.addIfMissingStatMonitor(maHomePartitionTotalSpace);
        Common.MONITORED_VALUES.addIfMissingStatMonitor(maHomePartitionUsableSpace);
        
        Common.MONITORED_VALUES.addIfMissingStatMonitor(noSqlPartitionTotalSpace);
        Common.MONITORED_VALUES.addIfMissingStatMonitor(noSqlPartitionUsableSpace);
        
        Common.MONITORED_VALUES.addIfMissingStatMonitor(sqlPartitionTotalSpace);
        Common.MONITORED_VALUES.addIfMissingStatMonitor(sqlPartitionUsableSpace);
        
        Common.MONITORED_VALUES.addIfMissingStatMonitor(filestorePartitionTotalSpace);
        Common.MONITORED_VALUES.addIfMissingStatMonitor(filestorePartitionUsableSpace);
        
        Common.MONITORED_VALUES.addIfMissingStatMonitor(maHomeSize);
        
        this.fileStoreMonitors = new HashMap<>();
        //Setup all filestores
        ModuleRegistry.getFileStoreDefinitions().values().stream().forEach( fs-> {
            LongMonitor monitor = new LongMonitor(FILE_STORE_SIZE_PREFIX + fs.getStoreName(), new TranslatableMessage("internal.monitor.fileStoreSize", fs.getStoreDescription()), this);
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
        
        //Volume information for MA Home Partition
        try {
            FileStore store = Files.getFileStore(Common.MA_HOME_PATH);
            maHomePartitionTotalSpace.setValue(store.getTotalSpace());
            maHomePartitionUsableSpace.setValue(store.getUsableSpace());
        }catch(Exception e) {
            LOG.error("Unable to get MA_HOME partition usage", e);
        }
        //Volume information for NoSQL partition 
        try {
            FileStore store = Files.getFileStore(Paths.get(NoSQLProxy.getDatabasePath()).toAbsolutePath());
            noSqlPartitionTotalSpace.setValue(store.getTotalSpace());
            noSqlPartitionUsableSpace.setValue(store.getUsableSpace());
        }catch(Exception e) {
            LOG.error("Unable to get NoSQL partition usage", e);
        }

        //Volume information for sql db partition (if possible)
        File dataDirectory = Common.databaseProxy.getDataDirectory();
        if(dataDirectory != null) {
            try{
                FileStore store = Files.getFileStore(dataDirectory.toPath());
                sqlPartitionTotalSpace.setValue(store.getTotalSpace());
                sqlPartitionUsableSpace.setValue(store.getUsableSpace());
            }catch(Exception e) {
                LOG.error("Unable to get Filestore partition usage", e);
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
        }catch(Exception e) {
            LOG.error("Unable to get Filestore partition usage", e);
        }
        
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
