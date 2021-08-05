/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.github.zafarkhaja.semver.Version;
import com.infiniteautomation.mango.monitor.ValueMonitor;
import com.infiniteautomation.mango.util.exception.FeatureDisabledException;
import com.infiniteautomation.mango.util.usage.AggregatePublisherUsageStatistics;
import com.infiniteautomation.mango.util.usage.DataPointUsageStatistics;
import com.infiniteautomation.mango.util.usage.DataSourceUsageStatistics;
import com.infiniteautomation.mango.util.usage.PublisherPointsUsageStatistics;
import com.infiniteautomation.mango.util.usage.PublisherUsageStatistics;
import com.serotonin.db.pair.StringStringPair;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonWriter;
import com.serotonin.json.type.JsonArray;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonString;
import com.serotonin.json.type.JsonTypeReader;
import com.serotonin.json.type.JsonValue;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.ICoreLicense;
import com.serotonin.m2m2.UpgradeVersionState;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.db.dao.PublisherDao;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.Module;
import com.serotonin.m2m2.module.ModuleNotificationListener;
import com.serotonin.m2m2.module.ModuleNotificationListener.UpgradeState;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.provider.Providers;
import com.serotonin.web.http.HttpUtils4;

/**
 * Module upgrade management service
 *
 * @author Terry Packer
 */
@Service
public class ModulesService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final List<ModuleNotificationListener> listeners = new CopyOnWriteArrayList<>();

    private UpgradeDownloader upgradeDownloader;
    private final Object upgradeDownloaderLock = new Object();

    private final Environment env;
    private final PermissionService permissionService;

    public ModulesService(Environment env, PermissionService permissionService) {
        this.env = env;
        this.permissionService = permissionService;

        // Add our own listener
        listeners.add(new ModulesServiceListener());
    }

    /**
     * Start downloading modules
     */
    public String startDownloads(List<StringStringPair> modules, boolean backup, boolean restart) {
        permissionService.ensureAdminRole(Common.getUser());

        if (env.getProperty("store.disableUpgrades", Boolean.class, false)) {
            throw new FeatureDisabledException(new TranslatableMessage("modules.error.upgradesDisabled"));
        }

        synchronized (upgradeDownloaderLock) {
            if (upgradeDownloader != null)
                return Common.translate("modules.versionCheck.occupied");
        }

        // Check if the selected modules will result in a version-consistent system.
        try {
            // Create the request
            Map<String, Object> json = new HashMap<>();
            Map<String, String> jsonModules = new HashMap<>();
            json.put("modules", jsonModules);

            Version coreVersion = Common.getVersion();

            jsonModules.put(ModuleRegistry.CORE_MODULE_NAME, coreVersion.toString());
            for (StringStringPair module : modules)
                jsonModules.put(module.getKey(), module.getValue());

            StringWriter stringWriter = new StringWriter();
            new JsonWriter(Common.JSON_CONTEXT, stringWriter).writeObject(json);
            String requestData = stringWriter.toString();

            // Send the request
            String baseUrl = env.getProperty("store.url");
            if (StringUtils.isEmpty(baseUrl)) {
                log.info("No consistency check performed as no store.url is defined in configuration file.");
                return "No consistency check performed as no store.url is defined in configuration file.";
            }
            baseUrl += "/servlet/consistencyCheck";

            HttpPost post = new HttpPost(baseUrl);
            post.setEntity(new StringEntity(requestData));

            String responseData = HttpUtils4.getTextContent(Common.getHttpClient(), post, 1);

            // Parse the response
            JsonTypeReader jsonReader = new JsonTypeReader(responseData);
            String result = jsonReader.read().toString();
            if (!"ok".equals(result))
                return result;
        } catch (Exception e) {
            log.error("", e);
            return e.getMessage();
        }

        synchronized (upgradeDownloaderLock) {
            // Ensure that 2 downloads cannot start at the same time.
            if (upgradeDownloader == null) {
                upgradeDownloader = new UpgradeDownloader(modules, backup, restart, listeners, () -> {
                    synchronized (upgradeDownloaderLock) {
                        upgradeDownloader = null;
                    }
                });
                //Clear out common info
                resetUpgradeStatus();
                Common.backgroundProcessing.execute(upgradeDownloader);
            } else
                return Common.translate("modules.versionCheck.occupied");
        }

        return null;
    }

    /**
     * Try and Cancel the Upgrade
     *
     * @return true if cancelled, false if not running
     */
    public boolean tryCancelUpgrade() {
        permissionService.ensureAdminRole(Common.getUser());
        synchronized (upgradeDownloaderLock) {
            if (upgradeDownloader == null)
                return false;
            else {
                upgradeDownloader.cancel();
                return true;
            }
        }
    }

    public UpgradeStatus monitorDownloads() {
        permissionService.ensureAdminRole(Common.getUser());
        UpgradeStatus status = new UpgradeStatus();
        synchronized (upgradeDownloaderLock) {
            if (upgradeDownloader == null && stage == UpgradeState.IDLE) {
                status.setStage(stage);
                return status;
            }
            status.setFinished(finished);
            status.setCancelled(cancelled);
            status.setRestart(restart);
            status.setError(error);
            status.setStage(stage);
            status.setResults(getUpgradeResults());

            if (finished)
                stage = UpgradeState.IDLE;
        }

        return status;
    }

    /**
     * How many upgrades are available
     */
    public int upgradesAvailable() throws Exception {
        permissionService.ensureAdminRole(Common.getUser());

        if (env.getProperty("store.disableUpgrades", Boolean.class, false)) {
            throw new FeatureDisabledException(new TranslatableMessage("modules.error.upgradesDisabled"));
        }

        JsonValue jsonResponse = getAvailableUpgrades();

        if (jsonResponse == null)
            return 0; //Empty store.url
        if (jsonResponse instanceof JsonString)
            throw new Exception("Mango Store Response Error: " + jsonResponse.toString());

        JsonObject root = jsonResponse.toJsonObject();

        int size = root.getJsonArray("upgrades").size();
        if (size > 0) {
            // Notify the listeners
            JsonValue jsonUpgrades = root.get("upgrades");
            JsonArray jsonUpgradesArray = jsonUpgrades.toJsonArray();
            for (JsonValue v : jsonUpgradesArray) {
                for (ModuleNotificationListener l : listeners)
                    l.moduleUpgradeAvailable(v.getJsonValue("name").toString(),
                            v.getJsonValue("version").toString());
            }
            JsonValue jsonInstalls = root.get("newInstalls");
            JsonArray jsonInstallsArray = jsonInstalls.toJsonArray();
            for (JsonValue v : jsonInstallsArray) {
                for (ModuleNotificationListener l : listeners)
                    l.newModuleAvailable(v.getJsonValue("name").toString(),
                            v.getJsonValue("version").toString());
            }

        }
        return size;
    }

    /**
     * Get the information for available upgrades
     */
    public JsonValue getAvailableUpgrades() throws JsonException, IOException, HttpException {
        permissionService.ensureAdminRole(Common.getUser());

        if (env.getProperty("store.disableUpgrades", Boolean.class, false)) {
            throw new FeatureDisabledException(new TranslatableMessage("modules.error.upgradesDisabled"));
        }

        // Create the request
        List<Module> modules = ModuleRegistry.getModules();
        Module.sortByName(modules);

        Map<String, Object> json = new HashMap<>();
        json.put("guid", Providers.get(ICoreLicense.class).getGuid());
        json.put("description", SystemSettingsDao.instance.getValue(SystemSettingsDao.INSTANCE_DESCRIPTION));
        json.put("distributor", env.getProperty("distributor"));
        json.put("upgradeVersionState",
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.UPGRADE_VERSION_STATE));

        Properties props = new Properties();
        Path propFile = Common.MA_HOME_PATH.resolve("release.properties");
        int versionState = UpgradeVersionState.DEVELOPMENT;
        if (Files.isRegularFile(propFile)) {
            try (InputStream in = Files.newInputStream(propFile)) {
                props.load(in);
            }
            String currentVersionState = props.getProperty("versionState");
            try {
                if (currentVersionState != null)
                    versionState = Integer.parseInt(currentVersionState);
            } catch (NumberFormatException e) {
            }
        }
        json.put("currentVersionState", versionState);

        Map<String, String> jsonModules = new HashMap<>();
        json.put("modules", jsonModules);

        Version coreVersion = Common.getVersion();
        jsonModules.put(ModuleRegistry.CORE_MODULE_NAME, coreVersion.toString());
        for (Module module : modules)
            if (!module.isMarkedForDeletion())
                jsonModules.put(module.getName(), module.getVersion().toString());

        // Add in the unloaded modules so we don't re-download them if we don't have to
        for (Module module : ModuleRegistry.getUnloadedModules())
            if (!module.isMarkedForDeletion())
                jsonModules.put(module.getName(), module.getVersion().toString());

        //Optionally Add Usage Data Check if first login for admin has happened to ensure they have
        if (SystemSettingsDao.instance.getBooleanValue(SystemSettingsDao.USAGE_TRACKING_ENABLED)) {
            //Collect statistics
            List<DataSourceUsageStatistics> dataSourceCounts = DataSourceDao.getInstance().getUsage();
            json.put("dataSourcesUsage", dataSourceCounts);
            List<DataPointUsageStatistics> dataPointCounts = DataPointDao.getInstance().getUsage();
            json.put("dataPointsUsage", dataPointCounts);
            AggregatePublisherUsageStatistics stats = PublisherDao.getInstance().getUsage();
            List<PublisherUsageStatistics> publisherCounts = stats.getPublisherUsageStatistics();
            json.put("publishersUsage", publisherCounts);
            List<PublisherPointsUsageStatistics> publisherPointsCounts = stats.getPublisherPointsUsageStatistics();
            json.put("publisherPointsUsage", publisherPointsCounts);

            for (ValueMonitor<?> m : Common.MONITORED_VALUES.getMonitors()) {
                if (m.isUploadToStore()) {
                    if (m.getValue() != null) {
                        json.put(m.getId(), m.getValue());
                    }
                }
            }
        }

        StringWriter stringWriter = new StringWriter();
        new JsonWriter(Common.JSON_CONTEXT, stringWriter).writeObject(json);
        String requestData = stringWriter.toString();

        // Send the request
        String baseUrl = env.getProperty("store.url");
        if (StringUtils.isEmpty(baseUrl)) {
            log.info("No version check performed as no store.url is defined in configuration file.");
            return null;
        }
        baseUrl += "/servlet/versionCheck";

        HttpPost post = new HttpPost(baseUrl);
        post.setEntity(new StringEntity(requestData));

        String responseData = HttpUtils4.getTextContent(Common.getHttpClient(), post, 1);

        // Parse the response
        JsonTypeReader jsonReader = new JsonTypeReader(responseData);
        return jsonReader.read();
    }

    public void addModuleNotificationListener(ModuleNotificationListener listener) {
        permissionService.ensureAdminRole(Common.getUser());
        listeners.add(listener);
    }

    public void removeModuleNotificationListener(ModuleNotificationListener listener) {
        permissionService.ensureAdminRole(Common.getUser());
        listeners.remove(listener);
    }

    //For status about upgrade state (Preferably use your own listener)
    private volatile UpgradeState stage = UpgradeState.IDLE;
    private volatile boolean cancelled;
    private volatile boolean finished;
    private volatile boolean restart;
    private volatile String error = null;
    private final List<StringStringPair> moduleResults = Collections.synchronizedList(new ArrayList<>());

    protected void resetUpgradeStatus() {
        cancelled = false;
        finished = false;
        restart = false;
        error = null;
        moduleResults.clear();
    }

    public List<StringStringPair> getUpgradeResults() {
        permissionService.ensureAdminRole(Common.getUser());
        return new ArrayList<>(moduleResults);
    }

    private class ModulesServiceListener implements ModuleNotificationListener {

        @Override
        public void moduleDownloaded(String name, String version) {
            moduleResults.add(new StringStringPair(name, Common.translate("modules.downloadComplete")));
        }

        @Override
        public void moduleDownloadFailed(String name, String version, String reason) {
            moduleResults.add(new StringStringPair(name, reason));
        }

        @Override
        public void moduleUpgradeAvailable(String name, String version) {
            //No-op
        }

        @Override
        public void upgradeStateChanged(UpgradeState state) {
            stage = state;
            switch (stage) {
                case CANCELLED:
                    cancelled = true;
                    break;
                case RESTART:
                    restart = true;
                    break;
                default:
                    break;
            }
        }

        @Override
        public void upgradeError(String e) {
            error = e;
        }

        @Override
        public void upgradeTaskFinished() {
            finished = true;
        }

        @Override
        public void newModuleAvailable(String name, String version) {
            //no-op
        }
    }

    public static class UpgradeStatus {
        private UpgradeState stage;
        private boolean finished;
        private boolean cancelled;
        private boolean restart;
        private String error;
        private List<StringStringPair> results = new ArrayList<>();

        public UpgradeState getStage() {
            return stage;
        }

        public void setStage(UpgradeState stage) {
            this.stage = stage;
        }

        public boolean isFinished() {
            return finished;
        }

        public void setFinished(boolean finished) {
            this.finished = finished;
        }

        public boolean isCancelled() {
            return cancelled;
        }

        public void setCancelled(boolean cancelled) {
            this.cancelled = cancelled;
        }

        public boolean isRestart() {
            return restart;
        }

        public void setRestart(boolean restart) {
            this.restart = restart;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public List<StringStringPair> getResults() {
            return results;
        }

        public void setResults(List<StringStringPair> results) {
            this.results = results;
        }

    }
}
