/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.mvc.controller;

import java.io.File;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.View;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.io.StreamUtils;
import com.serotonin.json.JsonWriter;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.ICoreLicense;
import com.serotonin.m2m2.ILifecycle;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.LicenseDefinition;
import com.serotonin.m2m2.module.Module;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.web.mvc.UrlHandler;
import com.serotonin.provider.Providers;
import com.serotonin.web.http.HttpUtils4;

public class ModulesController implements UrlHandler {
    private static final Log LOG = LogFactory.getLog(ModulesController.class);

    @Override
    public View handleRequest(HttpServletRequest request, HttpServletResponse response, Map<String, Object> model)
            throws Exception {
        // Check for a license download token.
        String token = request.getParameter("token");
        if (!StringUtils.isEmpty(token)) {
            if (downloadLicense(token))
                model.put("licenseDownloaded", true);
        }

        List<Module> modules = ModuleRegistry.getModules();
        Module.sortByName(modules);

        String version = Common.getVersion().getFullString();

        //Get the build number if one exists
        InputStream inStream = this.getClass().getResourceAsStream("/mango.build.number");
        if (inStream != null) {
            Properties props = new Properties();
            props.load(inStream);
            version += " build " + props.getProperty("build.number");
            inStream.close();
        }

        Module core = new Module("core", version, new TranslatableMessage("modules.core.description"),
                "Infinite Automation Systems.", "http://infiniteautomation.com", null, -1);
        
        core.setLicenseType(Common.license() == null ? null : Common.license().getLicenseType());
        core.addDefinition((LicenseDefinition) Providers.get(ICoreLicense.class));
        modules.add(0, core);

        model.put("guid", Providers.get(ICoreLicense.class).getGuid());
        model.put("distributor", Common.envProps.getString("distributor"));
        model.put("modules", modules);
        
        //Add in the Unloaded modules
        model.put("unloadedModules", ModuleRegistry.getUnloadedModules());

        // The JSON
        Map<String, Object> json = new HashMap<>();
        json.put("guid", Providers.get(ICoreLicense.class).getGuid());
        json.put("description", SystemSettingsDao.getValue(SystemSettingsDao.INSTANCE_DESCRIPTION));
        json.put("distributor", Common.envProps.getString("distributor"));

        Map<String, String> jsonModules = new HashMap<>();
        json.put("modules", jsonModules);

        for (Module module : modules) {
            if (module.getName().equals("core")) {
                //Don't add that core module as it might have the build number in the version
                jsonModules.put("core", Common.getVersion().getFullString());
            }
            else {
                jsonModules.put(module.getName(), module.getVersion());
            }
        }

        try {
            StringWriter out = new StringWriter();
            JsonWriter jsonWriter = new JsonWriter(Common.JSON_CONTEXT, out);
            jsonWriter.writeObject(json);
            model.put("json", out.toString());
        }
        catch (Exception e) {
            throw new ShouldNeverHappenException(e);
        }

        return null;
    }

    private boolean downloadLicense(String token) throws Exception {
        // Send the request
        String url = Common.envProps.getString("store.url");
        url += "/servlet/downloadLicense?token=" + token;

        String responseData = HttpUtils4.getTextContent(Common.getHttpClient(), url);

        // Should be an XML file. If it doesn't start with "<", it's an error.
        if (!responseData.startsWith("<")) {
            // Only log as info, because refreshes of a page where a previous download was successful will result in
            // an error being returned.
            LOG.info("License download failed: " + responseData);
            return false;
        }

        // If there is an existing license file, move it to a backup name. First check if the backup name exists, and 
        // if so, delete it.
        File licenseFile = new File(Common.MA_HOME, "m2m2.license.xml");
        File backupFile = new File(Common.MA_HOME, "m2m2.license.old.xml");

        if (licenseFile.exists()) {
            if (backupFile.exists())
                backupFile.delete();
            licenseFile.renameTo(backupFile);
        }

        // Save the data
        StreamUtils.writeFile(licenseFile, responseData);

        // Reload the license file.
        Providers.get(ILifecycle.class).loadLic();

        return true;
    }
}
