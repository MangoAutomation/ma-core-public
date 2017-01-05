/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.dwr;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.pair.StringStringPair;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonWriter;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonString;
import com.serotonin.json.type.JsonTypeReader;
import com.serotonin.json.type.JsonValue;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.Constants;
import com.serotonin.m2m2.ICoreLicense;
import com.serotonin.m2m2.ILifecycle;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.module.Module;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.rt.maint.work.BackupWorkItem;
import com.serotonin.m2m2.rt.maint.work.DatabaseBackupWorkItem;
import com.serotonin.m2m2.shared.ModuleUtils;
import com.serotonin.m2m2.util.timeout.TimeoutTask;
import com.serotonin.m2m2.web.dwr.util.DwrPermission;
import com.serotonin.provider.Providers;
import com.serotonin.web.http.HttpUtils4;

public class ModulesDwr extends BaseDwr {
    static final Log LOG = LogFactory.getLog(ModulesDwr.class);
    private static TimeoutTask SHUTDOWN_TASK;
    private static UpgradeDownloader UPGRADE_DOWNLOADER;

    @DwrPermission(admin = true)
    public boolean toggleDeletion(String moduleName) {
        Module module = ModuleRegistry.getModule(moduleName);
        module.setMarkedForDeletion(!module.isMarkedForDeletion());
        return module.isMarkedForDeletion();
    }

    @DwrPermission(admin = true)
    synchronized public static ProcessResult scheduleRestart() {
        ProcessResult result = new ProcessResult();
        if (SHUTDOWN_TASK == null) {
            long timeout = Common.getMillis(Common.TimePeriods.SECONDS, 10);
            ILifecycle lifecycle = Providers.get(ILifecycle.class);
            SHUTDOWN_TASK = lifecycle.scheduleShutdown(timeout, true);
            //Get the redirect page
            result.addData("shutdownUri", "/shutdown.htm");
        }
        else {
            result.addData("message", Common.translate("modules.restartAlreadyScheduled"));
        }

        return result;
    }

    @DwrPermission(admin = true)
    synchronized public static ProcessResult scheduleShutdown() {
        ProcessResult result = new ProcessResult();
        if (SHUTDOWN_TASK == null) {
            long timeout = Common.getMillis(Common.TimePeriods.SECONDS, 10);

            //Ensure our lifecycle state is set to PRE_SHUTDOWN
            ILifecycle lifecycle = Providers.get(ILifecycle.class);
            SHUTDOWN_TASK = lifecycle.scheduleShutdown(timeout, false);
            //Get the redirect page
            result.addData("shutdownUri", "/shutdown.htm");
        }
        else {
            result.addData("message", Common.translate("modules.shutdownAlreadyScheduled"));
        }

        return result;
    }

    @DwrPermission(admin = true)
    public ProcessResult versionCheck() {
        if (UPGRADE_DOWNLOADER != null) {
            ProcessResult result = new ProcessResult();
            result.addData("error", Common.translate("modules.versionCheck.occupied"));
            return result;
        }

        ProcessResult result = new ProcessResult();

        try {
            JsonValue jsonResponse = getAvailableUpgrades();

            if (jsonResponse instanceof JsonString)
                result.addData("error", jsonResponse.toString());
            else {
                JsonObject root = jsonResponse.toJsonObject();
                result.addData("upgrades", root.get("upgrades").toNative());
                result.addData("newInstalls", root.get("newInstalls").toNative());
            }
        }
        catch (Exception e) {
            LOG.error("", e);
            result.addData("error", e.getMessage());
        }

        return result;
    }

    @DwrPermission(admin = true)
    public String startDownloads(List<StringStringPair> modules, boolean backup, boolean restart) {
        if (UPGRADE_DOWNLOADER != null && !UPGRADE_DOWNLOADER.isFinished())
            return Common.translate("modules.versionCheck.occupied");

        // Check if the selected modules will result in a version-consistent system.
        try {
            // Create the request
            Map<String, Object> json = new HashMap<>();
            Map<String, String> jsonModules = new HashMap<>();
            json.put("modules", jsonModules);

            jsonModules.put("core", Common.getVersion().getFullString());
            for (StringStringPair module : modules)
                jsonModules.put(module.getKey(), module.getValue());

            StringWriter stringWriter = new StringWriter();
            new JsonWriter(Common.JSON_CONTEXT, stringWriter).writeObject(json);
            String requestData = stringWriter.toString();

            // Send the request
            String baseUrl = Common.envProps.getString("store.url");
            baseUrl += "/servlet/consistencyCheck";

            HttpPost post = new HttpPost(baseUrl);
            post.setEntity(new StringEntity(requestData));

            String responseData = HttpUtils4.getTextContent(Common.getHttpClient(), post, 1);

            // Parse the response
            JsonTypeReader jsonReader = new JsonTypeReader(responseData);
            String result = jsonReader.read().toString();
            if (!"ok".equals(result))
                return result;
        }
        catch (Exception e) {
            LOG.error("", e);
            return e.getMessage();
        }

        // Ensure that 2 downloads cannot start at the same time.
        if (UPGRADE_DOWNLOADER == null || UPGRADE_DOWNLOADER.isFinished()) {
            synchronized (this) {
                if (UPGRADE_DOWNLOADER == null || UPGRADE_DOWNLOADER.isFinished()) {
                    UPGRADE_DOWNLOADER = new UpgradeDownloader(modules, backup, restart);
                    Common.timer.execute(UPGRADE_DOWNLOADER);
                }
                else
                    return Common.translate("modules.versionCheck.occupied");
            }
        }

        return null;
    }
    
    @DwrPermission(admin = true)
    public ProcessResult tryCancelDownloads() {
    	ProcessResult pr = new ProcessResult();
    	if(UPGRADE_DOWNLOADER == null) {
    		pr.addGenericMessage("modules.versionCheck.notRunning");
    		return pr;
    	}
    	
    	UPGRADE_DOWNLOADER.cancel();
    	pr.addGenericMessage("common.cancelled");
    	return pr;
    }

    @DwrPermission(admin = true)
    public ProcessResult monitorDownloads() {
        ProcessResult result = new ProcessResult();
        result.addData("finished", UPGRADE_DOWNLOADER.isFinished());
        result.addData("cancelled", UPGRADE_DOWNLOADER.cancelled);
        result.addData("restart", UPGRADE_DOWNLOADER.isRestart());
        if (UPGRADE_DOWNLOADER.getError() != null)
            result.addData("error", UPGRADE_DOWNLOADER.getError());
        result.addData("stage", UPGRADE_DOWNLOADER.getStage());
        result.addData("results", UPGRADE_DOWNLOADER.getResults(getTranslations()));

        if (UPGRADE_DOWNLOADER.isFinished())
            UPGRADE_DOWNLOADER = null;

        return result;
    }

    /**
     * How many upgrades are available
     * @return
     * @throws Exception
     */
    public static int upgradesAvailable() throws Exception {
        JsonValue jsonResponse = getAvailableUpgrades();

        if (jsonResponse instanceof JsonString)
            throw new Exception("Mango Store Response Error: " + jsonResponse.toString());

        JsonObject root = jsonResponse.toJsonObject();

        return root.getJsonArray("upgrades").size();
    }

    public static JsonValue getAvailableUpgrades() throws JsonException, IOException, HttpException {
        // Create the request
        List<Module> modules = ModuleRegistry.getModules();
        Module.sortByName(modules);

        Map<String, Object> json = new HashMap<>();
        json.put("guid", Providers.get(ICoreLicense.class).getGuid());
        json.put("description", SystemSettingsDao.getValue(SystemSettingsDao.INSTANCE_DESCRIPTION));
        json.put("distributor", Common.envProps.getString("distributor"));

        Map<String, String> jsonModules = new HashMap<>();
        json.put("modules", jsonModules);

        jsonModules.put("core", Common.getVersion().getFullString());
        for (Module module : modules)
            jsonModules.put(module.getName(), module.getVersion());

        StringWriter stringWriter = new StringWriter();
        new JsonWriter(Common.JSON_CONTEXT, stringWriter).writeObject(json);
        String requestData = stringWriter.toString();

        // Send the request
        String baseUrl = Common.envProps.getString("store.url");
        baseUrl += "/servlet/versionCheck";

        HttpPost post = new HttpPost(baseUrl);
        post.setEntity(new StringEntity(requestData));

        String responseData = HttpUtils4.getTextContent(Common.getHttpClient(), post, 1);

        // Parse the response
        JsonTypeReader jsonReader = new JsonTypeReader(responseData);
        return jsonReader.read();
    }

    class UpgradeDownloader implements Runnable {
    	
        private final List<StringStringPair> modules;
        private final boolean backup;
        private final boolean restart;
        private final List<StringStringPair> results = new ArrayList<>();
        private String stage = Common.translate("modules.downloadUpgrades.stage.start");
        private String error;
        private boolean finished;
        private final File coreDir = new File(Common.MA_HOME);
        private final File moduleDir = new File(coreDir, Constants.DIR_WEB + "/" + Constants.DIR_MODULES);
        private volatile boolean cancelled = false;

        public UpgradeDownloader(List<StringStringPair> modules, boolean backup, boolean restart) {
            this.modules = modules;
            this.backup = backup;
            this.restart = restart;
        }
        
        void cancel() {
        	this.cancelled = true;
        }

        @Override
        public void run() {
        	LOG.info("UpgradeDownloader started");
        	
            if (backup) {
                // Run the backup.
                stage = Common.translate("modules.downloadUpgrades.stage.backup");
                LOG.info("UpgradeDownloader: " + stage);
                
                // Do the backups. They run async, so this returns immediately. The shutdown will wait for the 
                // background processes to finish though.
                BackupWorkItem.queueBackup(SystemSettingsDao.getValue(SystemSettingsDao.BACKUP_FILE_LOCATION));
                DatabaseBackupWorkItem.queueBackup(SystemSettingsDao
                        .getValue(SystemSettingsDao.DATABASE_BACKUP_FILE_LOCATION));
            }

            stage = Common.translate("modules.downloadUpgrades.stage.download");
            LOG.info("UpgradeDownloader: " + stage);
            
            HttpClient httpClient = Common.getHttpClient();

            // Create the temp directory into which to download, if necessary.
            File tempDir = new File(Common.MA_HOME, ModuleUtils.DOWNLOAD_DIR);
            if (!tempDir.exists())
                tempDir.mkdirs();

            // Delete anything that is currently the temp directory.
            try {
                FileUtils.cleanDirectory(tempDir);
            }
            catch (IOException e) {
                error = "Error while clearing temp dir: " + e.getMessage();
                LOG.warn("Error while clearing temp dir", e);
                finished = true;
                return;
            }

            // Delete all upgrade files of any version from the target directories. Only do this for names in the
            // modules list so that custom modules do not get deleted.
            cleanDownloads();

            for (StringStringPair mod : modules) {
            	if(cancelled) {
            		LOG.info("UpgradeDownloader: Cancelled");
            		try {
                        FileUtils.cleanDirectory(tempDir);
                    }
                    catch (IOException e) {
                        error = "Error while clearing temp dir when cancelled: " + e.getMessage();
                        LOG.warn("Error while clearing temp dir when cancelled", e);
                        finished = true;
                    }
            		return;
            	}
                String name = mod.getKey();
                String version = mod.getValue();

                String filename = ModuleUtils.moduleFilename(name, version);
                String url = Common.envProps.getString("store.url") + "/" + ModuleUtils.downloadFilename(name, version);
                HttpGet get = new HttpGet(url);

                FileOutputStream out = null;
                try {
                    out = new FileOutputStream(new File(tempDir, filename));
                    HttpUtils4.execute(httpClient, get, out);
                    synchronized (results) {
                        results.add(new StringStringPair(name, null));
                    }
                }
                catch (IOException e) {
                    synchronized (results) {
                        results.add(new StringStringPair(name, e.getMessage()));
                        LOG.warn("Upgrade download error", e);
                        error = "Download failed";
                        finished = true;
                        return;
                    }
                }
                finally {
                    try {
                        if (out != null)
                            out.close();
                    }
                    catch (IOException e) {
                        // no op
                    }
                }
            }

            stage = Common.translate("modules.downloadUpgrades.stage.install");
            LOG.info("UpgradeDownloader: " + stage);

            // Move the downloaded files to their proper places.
            File[] files = tempDir.listFiles();
            if (files == null || files.length == 0) {
                error = "Weird, there are no downloaded files to move";
                LOG.warn(error);
                finished = true;
                return;
            }

            for (File file : files) {
                File targetDir = file.getName().startsWith(ModuleUtils.Constants.MODULE_PREFIX + "core") ? coreDir
                        : moduleDir;
                try {
                    FileUtils.moveFileToDirectory(file, targetDir, false);
                }
                catch (IOException e) {
                    // If anything bad happens during the copy, try to clean out the download files again.
                    cleanDownloads();

                    LOG.warn(e);
                    throw new ShouldNeverHappenException(e);
                }
            }

            // Delete the download dir.
            try {
                FileUtils.deleteDirectory(tempDir);
            }
            catch (IOException e) {
                LOG.warn(e);
            }
            
            //Final chance to be cancelled....
            if(cancelled) {
            	//Delete what we downloaded.
            	cleanDownloads();
            	LOG.info("UpgradeDownloader: Cancelled");
            	finished = true;
            	return;
            }

            if (restart) {
                stage = Common.translate("modules.downloadUpgrades.stage.restart");
                LOG.info("UpgradeDownloader: " + stage);
                
                if (SHUTDOWN_TASK == null) {
                    ILifecycle lifecycle = Providers.get(ILifecycle.class);
                    SHUTDOWN_TASK = lifecycle.scheduleShutdown(5000, true);
                }
            }
            else{
                stage = Common.translate("modules.downloadUpgrades.stage.done");
                LOG.info("UpgradeDownloader: " + stage);
            }
            
            finished = true;
        }

        private void cleanDownloads() {
            for (StringStringPair mod : modules) {
                final String name = mod.getKey();
                File targetDir = name.equals("core") ? coreDir : moduleDir;
                File[] files = targetDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile() && file.getName().startsWith(ModuleUtils.Constants.MODULE_PREFIX + name))
                            file.delete();
                    }
                }
            }
        }

        public List<StringStringPair> getResults(Translations translations) {
            synchronized (results) {
                List<StringStringPair> l = new ArrayList<>();
                String m;
                for (StringStringPair kvp : results) {
                    if (kvp.getValue() == null)
                        m = translations.translate("modules.downloadComplete");
                    else
                        m = new TranslatableMessage("modules.downloadFailure", kvp.getValue()).translate(translations);
                    l.add(new StringStringPair(kvp.getKey(), m));
                }

                return l;
            }
        }

        public boolean isFinished() {
            return finished;
        }

        public boolean isRestart() {
            return restart;
        }

        public String getError() {
            return error;
        }

        public String getStage() {
            return stage;
        }

        public void setError(String error) {
            this.error = error;
        }

        public List<StringStringPair> getModules() {
            return modules;
        }
    }
}
