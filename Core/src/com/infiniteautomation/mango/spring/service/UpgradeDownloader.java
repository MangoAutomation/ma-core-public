/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import com.github.zafarkhaja.semver.Version;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.pair.StringStringPair;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.IMangoLifecycle;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.ModuleNotificationListener;
import com.serotonin.m2m2.module.ModuleNotificationListener.UpgradeState;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.rt.maint.work.BackupWorkItem;
import com.serotonin.m2m2.rt.maint.work.DatabaseBackupWorkItem;
import com.serotonin.m2m2.shared.ModuleUtils;
import com.serotonin.m2m2.util.timeout.HighPriorityTask;
import com.serotonin.provider.Providers;
import com.serotonin.web.http.HttpUtils4;

public class UpgradeDownloader extends HighPriorityTask {

    private final Log log = LogFactory.getLog(getClass());
    private final List<StringStringPair> modules;
    private final boolean backup;
    private final boolean restart;
    private final File coreDir = Common.MA_HOME_PATH.toFile();
    private final File moduleDir = Common.MODULES.toFile();
    private final List<ModuleNotificationListener> listeners;
    private final Runnable completionCallback;
    private volatile boolean cancelled = false;

    /**
     * Only allow 1 to be scheduled all others will be rejected
     */
    public UpgradeDownloader(List<StringStringPair> modules, boolean backup, boolean restart, List<ModuleNotificationListener> listeners, Runnable completionCallback) {
        super("Upgrade downloader", "UpgradeDownloader", 0);
        this.modules = modules;
        this.backup = backup;
        this.restart = restart;
        this.listeners = listeners;
        this.completionCallback = completionCallback;
    }

    @Override
    public boolean cancel() {
        this.cancelled = true;
        return super.cancel();
    }

    @Override
    public void run(long runtime) {
        try {
            for (ModuleNotificationListener listener : listeners)
                listener.upgradeStateChanged(UpgradeState.STARTED);
            log.info("UpgradeDownloader started");
            String baseStoreUrl = Common.envProps.getString("store.url");
            if (StringUtils.isEmpty(baseStoreUrl)) {
                log.info("Upgrade download not started as store.url is blank in env.properties.");
                for (ModuleNotificationListener listener : listeners)
                    listener.upgradeStateChanged(UpgradeState.DONE);
                return;
            }

            Future<Void> backupComplete;
            Future<Void> databaseBackupComplete;
            if (backup) {
                // Run the backup.
                log.info("UpgradeDownloader: " + UpgradeState.BACKUP);
                for (ModuleNotificationListener listener : listeners)
                    listener.upgradeStateChanged(UpgradeState.BACKUP);

                // Do the backups. They run async, so this returns immediately. The shutdown will
                // wait for the
                // background processes to finish though.
                backupComplete = BackupWorkItem.queueBackup(
                        SystemSettingsDao.instance.getValue(SystemSettingsDao.BACKUP_FILE_LOCATION));
                databaseBackupComplete = DatabaseBackupWorkItem.queueBackup(SystemSettingsDao.instance
                        .getValue(SystemSettingsDao.DATABASE_BACKUP_FILE_LOCATION));
            } else {
                backupComplete = CompletableFuture.completedFuture(null);
                databaseBackupComplete = CompletableFuture.completedFuture(null);
            }

            log.info("UpgradeDownloader: " + UpgradeState.DOWNLOAD);
            for (ModuleNotificationListener listener : listeners)
                listener.upgradeStateChanged(UpgradeState.DOWNLOAD);

            HttpClient httpClient = Common.getHttpClient();

            // Create the temp directory into which to download, if necessary.
            Path tempDirPath = Common.getTempPath().resolve(ModuleUtils.DOWNLOAD_DIR);
            File tempDir = tempDirPath.toFile();
            Files.createDirectories(tempDirPath);

            // Delete anything that is currently the temp directory.
            try {
                FileUtils.cleanDirectory(tempDir);
            } catch (IOException e) {
                String error = "Error while clearing temp dir: " + e.getMessage();
                log.warn("Error while clearing temp dir", e);
                for (ModuleNotificationListener listener : listeners)
                    listener.upgradeError(error);
                return;
            }

            // Delete all upgrade files of any version from the target directories. Only do this for
            // names in the
            // modules list so that custom modules do not get deleted.
            cleanDownloads();

            for (StringStringPair mod : modules) {
                if (cancelled) {
                    log.info("UpgradeDownloader: Cancelled");
                    try {
                        FileUtils.cleanDirectory(tempDir);
                    } catch (IOException e) {
                        String error = "Error while clearing temp dir when cancelled: " + e.getMessage();
                        log.warn("Error while clearing temp dir when cancelled", e);
                        for (ModuleNotificationListener listener : listeners)
                            listener.upgradeError(error);
                    }
                    for (ModuleNotificationListener listener : listeners)
                        listener.upgradeStateChanged(UpgradeState.CANCELLED);
                    return;
                }
                String name = mod.getKey();
                Version version = Version.valueOf(mod.getValue());

                String filename = ModuleUtils.moduleFilename(name, version.getNormalVersion());
                String url = baseStoreUrl + "/"
                        + ModuleUtils.downloadFilename(name, version.getNormalVersion());
                HttpGet get = new HttpGet(url);

                FileOutputStream out = null;
                File outFile = new File(tempDir, filename);
                try {
                    out = new FileOutputStream(outFile);
                    HttpUtils4.execute(httpClient, get, out);
                    for (ModuleNotificationListener listener : listeners)
                        listener.moduleDownloaded(name, version.toString());
                } catch (IOException e) {
                    log.warn("Upgrade download error", e);
                    String error = new TranslatableMessage("modules.downloadFailure", e.getMessage()).translate(Common.getTranslations());
                    //Notify of the failure
                    for (ModuleNotificationListener listener : listeners)
                        listener.moduleDownloadFailed(name, version.toString(), error);
                    return;
                } finally {
                    try {
                        if (out != null)
                            out.close();
                    } catch (IOException e) {
                        // no op
                    }
                }

                //Validate that module.properties exists and is properly readable
                if (!ModuleRegistry.CORE_MODULE_NAME.equals(name)) {
                    Map<String, String> expectedProperties = new HashMap<>();
                    expectedProperties.put("name", name);
                    expectedProperties.put("version", version.toString());
                    boolean signed = true;
                    try (ZipFile module = new ZipFile(outFile)) {
                        ZipEntry propertiesFile = module.getEntry(ModuleUtils.Constants.MODULE_SIGNED);
                        if (propertiesFile == null) {
                            signed = false;
                            propertiesFile = module.getEntry(ModuleUtils.Constants.MODULE_PROPERTIES);
                            if (propertiesFile == null) {
                                log.warn("Module missing properties file.");
                                throw new ShouldNeverHappenException("Module missing properties file.");
                            }
                        }
                        InputStream in = module.getInputStream(propertiesFile);
                        Common.verifyProperties(in, signed, expectedProperties);
                    } catch (Exception e) {
                        log.warn(e);
                        throw new ShouldNeverHappenException(e);
                    }
                }
            }

            log.info("UpgradeDownloader: " + UpgradeState.INSTALL);
            for (ModuleNotificationListener listener : listeners)
                listener.upgradeStateChanged(UpgradeState.INSTALL);

            // Move the downloaded files to their proper places.
            File[] files = tempDir.listFiles();
            if (files == null || files.length == 0) {
                String error = "Weird, there are no downloaded files to move";
                log.warn(error);
                for (ModuleNotificationListener listener : listeners)
                    listener.upgradeError(error);
                return;
            }

            for (File file : files) {
                File targetDir = file.getName().startsWith(ModuleUtils.Constants.MODULE_PREFIX + ModuleRegistry.CORE_MODULE_NAME) ? coreDir : moduleDir;

                try {
                    FileUtils.moveFileToDirectory(file, targetDir, false);
                } catch (IOException e) {
                    // If anything bad happens during the copy, try to clean out the download files
                    // again.
                    String error = e.getMessage();
                    for (ModuleNotificationListener listener : listeners)
                        listener.upgradeError(error);
                    cleanDownloads();
                    log.warn(e);
                    throw new ShouldNeverHappenException(e);
                }
            }

            // Delete the download dir.
            try {
                FileUtils.deleteDirectory(tempDir);
            } catch (IOException e) {
                log.warn(e);
            }

            // Final chance to be cancelled....
            if (cancelled) {
                // Delete what we downloaded.
                cleanDownloads();
                log.info("UpgradeDownloader: Cancelled");
                for (ModuleNotificationListener listener : listeners)
                    listener.upgradeStateChanged(UpgradeState.CANCELLED);
                return;
            }

            //Wait for backups before we shutdown
            try {
                backupComplete.get();
            } catch (Exception e) {
                log.error("Configuration Backup failed", e);
            }
            try {
                databaseBackupComplete.get();
            } catch (Exception e) {
                log.error("Database Backup failed", e);
            }

            if (restart) {
                log.info("UpgradeDownloader: " + UpgradeState.RESTART);
                IMangoLifecycle lifecycle = Providers.get(IMangoLifecycle.class);
                lifecycle.scheduleShutdown(null, true, Common.getUser());
                for (ModuleNotificationListener listener : listeners)
                    listener.upgradeStateChanged(UpgradeState.RESTART);

            } else {
                log.info("UpgradeDownloader: " + UpgradeState.DONE);
                for (ModuleNotificationListener listener : listeners)
                    listener.upgradeStateChanged(UpgradeState.DONE);
            }
        } catch (Exception e) {
            log.error("Error in upgrade process", e);
        } finally {
            for (ModuleNotificationListener listener : listeners) {
                try {
                    listener.upgradeTaskFinished();
                } catch (Exception e) {
                }
            }
            completionCallback.run();
        }
    }

    private void cleanDownloads() throws IOException {
        for (StringStringPair mod : modules) {
            final String name = mod.getKey();
            File targetDir = name.equals(ModuleRegistry.CORE_MODULE_NAME) ? coreDir : moduleDir;
            File[] files = targetDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().startsWith(ModuleUtils.Constants.MODULE_PREFIX + name)) {
                        Files.delete(file.toPath());
                    }
                }
            }
        }
    }
}
