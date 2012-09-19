/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.mvc.controller;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.ParameterizableViewController;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.json.JsonWriter;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.ICoreLicense;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.LicenseDefinition;
import com.serotonin.m2m2.module.Module;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.provider.Providers;

public class ModulesController extends ParameterizableViewController {
    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> model = new HashMap<String, Object>();

        List<Module> modules = ModuleRegistry.getModules();
        Module.sortByName(modules);

        Module core = new Module("core", Common.getVersion().getFullString(), new TranslatableMessage(
                "modules.core.description"), "Serotonin Software Technologies, Inc and InfiniteAUTOMATION SYSTEMS.",
                "http://infiniteautomation.com", null);

        core.addDefinition((LicenseDefinition) Providers.get(ICoreLicense.class));
        modules.add(0, core);

        model.put("guid", Providers.get(ICoreLicense.class).getGuid());
        model.put("modules", modules);

        // The JSON
        Map<String, Object> json = new HashMap<String, Object>();
        json.put("guid", Providers.get(ICoreLicense.class).getGuid());
        json.put("description", SystemSettingsDao.getValue(SystemSettingsDao.INSTANCE_DESCRIPTION));
        json.put("distributor", Common.envProps.getString("distributor"));
        json.put("domain", ControllerUtils.getDomain(request));

        Map<String, String> jsonModules = new HashMap<String, String>();
        json.put("modules", jsonModules);

        for (Module module : modules)
            jsonModules.put(module.getName(), module.getVersion());

        try {
            StringWriter out = new StringWriter();
            JsonWriter jsonWriter = new JsonWriter(Common.JSON_CONTEXT, out);
            jsonWriter.writeObject(json);
            model.put("json", out.toString());
        }
        catch (Exception e) {
            throw new ShouldNeverHappenException(e);
        }

        return new ModelAndView(getViewName(), model);
    }
}
