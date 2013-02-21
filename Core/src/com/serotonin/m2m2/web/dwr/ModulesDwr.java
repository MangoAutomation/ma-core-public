/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.dwr;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.pair.StringStringPair;
import com.serotonin.json.JsonWriter;
import com.serotonin.json.type.JsonObject;
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
import com.serotonin.m2m2.rt.event.type.SystemEventType;
import com.serotonin.m2m2.shared.ModuleUtils;
import com.serotonin.m2m2.util.timeout.TimeoutClient;
import com.serotonin.m2m2.util.timeout.TimeoutTask;
import com.serotonin.m2m2.web.dwr.util.DwrPermission;
import com.serotonin.provider.Providers;
import com.serotonin.web.http.HttpUtils4;

public class ModulesDwr extends BaseDwr {
    static final Log LOG = LogFactory.getLog(ModulesDwr.class);
    private static TimeoutTask RESTART_TASK;
    private static UpgradeDownloader UPGRADE_DOWNLOADER;

    @DwrPermission(admin = true)
    public boolean toggleDeletion(String moduleName) {
        Module module = ModuleRegistry.getModule(moduleName);
        module.setMarkedForDeletion(!module.isMarkedForDeletion());
        return module.isMarkedForDeletion();
    }

    @DwrPermission(admin = true)
    synchronized public void scheduleRestart() {
        if (RESTART_TASK == null) {
            SystemEventType.raiseEvent(new SystemEventType(SystemEventType.TYPE_SYSTEM_SHUTDOWN), System
                    .currentTimeMillis(), false, new TranslatableMessage("modules.restartScheduledBy", Common.getUser()
                    .getUsername()));

            long timeout = Common.getMillis(Common.TimePeriods.SECONDS, 10);
            RESTART_TASK = new TimeoutTask(timeout, new TimeoutClient() {
                @Override
                public void scheduleTimeout(long fireTime) {
                    File restartFlag = new File(Common.M2M2_HOME, "RESTART");
                    if (!restartFlag.exists()) {
                        try {
                            FileWriter fw = new FileWriter(restartFlag);
                            fw.write("restart");
                            fw.close();
                        }
                        catch (IOException e) {
                            LOG.error("Unabled to create restart flag file", e);
                        }
                    }
                    Providers.get(ILifecycle.class).terminate();
                }
            });
        }
    }

    @DwrPermission(admin = true)
    public List<StringStringPair> versionCheck() {
        if (UPGRADE_DOWNLOADER != null)
            return UPGRADE_DOWNLOADER.getModules();

        try {
            // Create the request
            List<Module> modules = ModuleRegistry.getModules();
            Module.sortByName(modules);

            Map<String, Object> json = new HashMap<String, Object>();
            json.put("guid", Providers.get(ICoreLicense.class).getGuid());
            json.put("description", SystemSettingsDao.getValue(SystemSettingsDao.INSTANCE_DESCRIPTION));
            json.put("distributor", Common.envProps.getString("distributor"));

            Map<String, String> jsonModules = new HashMap<String, String>();
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
            String responseData = HttpUtils4.getTextContent(Common.getHttpClient(), post);

            // Parse the response
            JsonTypeReader jsonReader = new JsonTypeReader(responseData);
            JsonObject root = jsonReader.read().toJsonObject();

            List<StringStringPair> upgrades = new ArrayList<StringStringPair>();
            for (Map.Entry<String, JsonValue> mod : root.entrySet()) {
                String name = mod.getKey();
                String version = mod.getValue().toString();
                upgrades.add(new StringStringPair(name, version));
            }

            return upgrades;
        }
        catch (Exception e) {
            throw new ShouldNeverHappenException(e);
        }
    }

    @DwrPermission(admin = true)
    public void startDownloads(List<StringStringPair> modules) {
        if (UPGRADE_DOWNLOADER == null) {
            UPGRADE_DOWNLOADER = new UpgradeDownloader(modules);
            Common.timer.execute(UPGRADE_DOWNLOADER);
        }
    }

    @DwrPermission(admin = true)
    public ProcessResult monitorDownloads() {
        ProcessResult result = new ProcessResult();
        result.addData("finished", UPGRADE_DOWNLOADER.isFinished());
        result.addData("results", UPGRADE_DOWNLOADER.getResults(getTranslations()));
        return result;
    }

    class UpgradeDownloader implements Runnable {
        private final List<StringStringPair> modules;
        private final List<StringStringPair> results = new ArrayList<StringStringPair>();
        private boolean finished;

        public UpgradeDownloader(List<StringStringPair> modules) {
            this.modules = modules;
        }

        @Override
        public void run() {
            HttpClient httpClient = Common.getHttpClient();

            for (StringStringPair mod : modules) {
                String name = mod.getKey();
                String version = mod.getValue();

                String filename = ModuleUtils.moduleFilename(name, version);
                String url = Common.envProps.getString("store.url") + "/" + ModuleUtils.downloadFilename(name, version);
                HttpGet get = new HttpGet(url);

                FileOutputStream out = null;
                try {
                    String saveDir = Common.M2M2_HOME;
                    if (!"core".equals(name))
                        saveDir += "/" + Constants.DIR_WEB + "/" + Constants.DIR_MODULES;
                    out = new FileOutputStream(new File(saveDir, filename));

                    HttpUtils4.execute(httpClient, get, out);

                    synchronized (results) {
                        results.add(new StringStringPair(name, null));
                    }
                }
                catch (IOException e) {
                    synchronized (results) {
                        results.add(new StringStringPair(name, e.getMessage()));
                        LOG.warn("Upgrade download error", e);
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

            finished = true;
        }

        public List<StringStringPair> getResults(Translations translations) {
            synchronized (results) {
                List<StringStringPair> l = new ArrayList<StringStringPair>();
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

        public List<StringStringPair> getModules() {
            return modules;
        }
    }
}
