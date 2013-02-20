/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.module;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.m2m2.module.license.LicenseEnforcement;
import com.serotonin.m2m2.vo.User;

/**
 * The registry of all modules in an MA instance.
 * 
 * @author Matthew Lohbihler
 */
public class ModuleRegistry {
    private static final Object LOCK = new Object();
    private static final Map<String, Module> MODULES = new LinkedHashMap<String, Module>();

    private static Map<String, DataSourceDefinition> DATA_SOURCE_DEFINITIONS;
    private static Map<String, PublisherDefinition> PUBLISHER_DEFINITIONS;
    private static Map<String, EventTypeDefinition> EVENT_TYPE_DEFINITIONS;
    private static Map<String, SystemEventTypeDefinition> SYSTEM_EVENT_TYPE_DEFINITIONS;
    private static Map<String, AuditEventTypeDefinition> AUDIT_EVENT_TYPE_DEFINITIONS;

    private static Map<UrlMappingDefinition.Permission, List<UrlMappingDefinition>> MENU_ITEMS;

    private static final List<LicenseEnforcement> licenseEnforcements = new ArrayList<LicenseEnforcement>();
    private static final List<ModuleElementDefinition> defaultDefinitions = new ArrayList<ModuleElementDefinition>();

    /**
     * @return a list of all available modules in the instance.
     */
    public static List<Module> getModules() {
        return new ArrayList<Module>(MODULES.values());
    }

    /**
     * Returns the instance of the module or null if not found for the given module name.
     * 
     * @param name
     *            the name of the module
     * @return the module instance or null if not found.
     */
    public static Module getModule(String name) {
        return MODULES.get(name);
    }

    public static boolean hasModule(String name) {
        return MODULES.containsKey(name);
    }

    /**
     * Should not be used by client code.
     */
    public static void addModule(Module module) {
        MODULES.put(module.getName(), module);
    }

    //
    //
    // Data source special handling
    //
    public static DataSourceDefinition getDataSourceDefinition(String type) {
        ensureDataSourceDefinitions();
        return DATA_SOURCE_DEFINITIONS.get(type);
    }

    public static Set<String> getDataSourceDefinitionTypes() {
        ensureDataSourceDefinitions();
        return DATA_SOURCE_DEFINITIONS.keySet();
    }

    private static void ensureDataSourceDefinitions() {
        if (DATA_SOURCE_DEFINITIONS == null) {
            synchronized (LOCK) {
                if (DATA_SOURCE_DEFINITIONS == null) {
                    DATA_SOURCE_DEFINITIONS = new HashMap<String, DataSourceDefinition>();
                    for (Module module : MODULES.values()) {
                        for (DataSourceDefinition def : module.getDefinitions(DataSourceDefinition.class))
                            DATA_SOURCE_DEFINITIONS.put(def.getDataSourceTypeName(), def);
                    }
                }
            }
        }
    }

    //
    //
    // Publisher special handling
    //
    public static PublisherDefinition getPublisherDefinition(String type) {
        ensurePublisherDefinitions();
        return PUBLISHER_DEFINITIONS.get(type);
    }

    public static Set<String> getPublisherDefinitionTypes() {
        ensurePublisherDefinitions();
        return PUBLISHER_DEFINITIONS.keySet();
    }

    private static void ensurePublisherDefinitions() {
        if (PUBLISHER_DEFINITIONS == null) {
            synchronized (LOCK) {
                if (PUBLISHER_DEFINITIONS == null) {
                    PUBLISHER_DEFINITIONS = new HashMap<String, PublisherDefinition>();
                    for (Module module : MODULES.values()) {
                        for (PublisherDefinition def : module.getDefinitions(PublisherDefinition.class))
                            PUBLISHER_DEFINITIONS.put(def.getPublisherTypeName(), def);
                    }
                }
            }
        }
    }

    //
    //
    // System event type special handling
    //
    public static SystemEventTypeDefinition getSystemEventTypeDefinition(String typeName) {
        ensureSystemEventTypeDefinitions();
        return SYSTEM_EVENT_TYPE_DEFINITIONS.get(typeName);
    }

    private static void ensureSystemEventTypeDefinitions() {
        if (SYSTEM_EVENT_TYPE_DEFINITIONS == null) {
            synchronized (LOCK) {
                if (SYSTEM_EVENT_TYPE_DEFINITIONS == null) {
                    SYSTEM_EVENT_TYPE_DEFINITIONS = new HashMap<String, SystemEventTypeDefinition>();
                    for (Module module : MODULES.values()) {
                        for (SystemEventTypeDefinition def : module.getDefinitions(SystemEventTypeDefinition.class))
                            SYSTEM_EVENT_TYPE_DEFINITIONS.put(def.getTypeName(), def);
                    }
                }
            }
        }
    }

    //
    //
    // Module event type special handling
    //
    public static EventTypeDefinition getEventTypeDefinition(String eventTypeName) {
        ensureEventTypeDefinitions();
        return EVENT_TYPE_DEFINITIONS.get(eventTypeName);
    }

    private static void ensureEventTypeDefinitions() {
        if (EVENT_TYPE_DEFINITIONS == null) {
            synchronized (LOCK) {
                if (EVENT_TYPE_DEFINITIONS == null) {
                    EVENT_TYPE_DEFINITIONS = new HashMap<String, EventTypeDefinition>();
                    for (Module module : MODULES.values()) {
                        for (EventTypeDefinition def : module.getDefinitions(EventTypeDefinition.class))
                            EVENT_TYPE_DEFINITIONS.put(def.getTypeName(), def);
                    }
                }
            }
        }
    }

    //
    //
    // Audit event type special handling
    //
    public static AuditEventTypeDefinition getAuditEventTypeDefinition(String typeName) {
        ensureAuditEventTypeDefinitions();
        return AUDIT_EVENT_TYPE_DEFINITIONS.get(typeName);
    }

    private static void ensureAuditEventTypeDefinitions() {
        if (AUDIT_EVENT_TYPE_DEFINITIONS == null) {
            synchronized (LOCK) {
                if (AUDIT_EVENT_TYPE_DEFINITIONS == null) {
                    AUDIT_EVENT_TYPE_DEFINITIONS = new HashMap<String, AuditEventTypeDefinition>();
                    for (Module module : MODULES.values()) {
                        for (AuditEventTypeDefinition def : module.getDefinitions(AuditEventTypeDefinition.class))
                            AUDIT_EVENT_TYPE_DEFINITIONS.put(def.getTypeName(), def);
                    }
                }
            }
        }
    }

    //
    //
    // Generic handling
    //
    public static <T extends ModuleElementDefinition> List<T> getDefinitions(Class<T> clazz) {
        List<T> defs = new ArrayList<T>();
        for (Module module : MODULES.values())
            defs.addAll(module.getDefinitions(clazz));
        defs.addAll(Module.getDefinitions(defaultDefinitions, clazz));
        return defs;
    }

    public static <T extends ModuleElementDefinition> T getDefinition(Class<T> clazz, boolean first) {
        List<T> defs = getDefinitions(clazz);
        if (defs.isEmpty())
            return null;
        if (first)
            return defs.get(0);
        return defs.get(defs.size() - 1);
    }

    /**
     * @return a list of all available locale names in this instance.
     */
    public static Set<String> getLocales() {
        Set<String> locales = new HashSet<String>();
        for (Module module : MODULES.values())
            locales.addAll(module.getLocales());
        return locales;
    }

    /**
     * @return a map by permissions type of all available menu items in this instance.
     */
    public static Map<UrlMappingDefinition.Permission, List<UrlMappingDefinition>> getMenuItems() {
        if (MENU_ITEMS == null) {
            synchronized (LOCK) {
                if (MENU_ITEMS == null) {
                    MENU_ITEMS = new HashMap<UrlMappingDefinition.Permission, List<UrlMappingDefinition>>();

                    for (UrlMappingDefinition url : getDefinitions(UrlMappingDefinition.class)) {
                        if (!StringUtils.isBlank(url.getMenuKey())) {
                            List<UrlMappingDefinition> permList = MENU_ITEMS.get(url.getPermission());

                            if (permList == null) {
                                permList = new ArrayList<UrlMappingDefinition>();
                                MENU_ITEMS.put(url.getPermission(), permList);
                            }

                            permList.add(url);
                        }
                    }
                }
            }
        }

        return MENU_ITEMS;
    }

    public static synchronized void addLicenseEnforcement(LicenseEnforcement licenseEnforcement) {
        licenseEnforcements.add(licenseEnforcement);
    }

    @SuppressWarnings("unchecked")
    public static <T extends LicenseEnforcement> List<T> getLicenseEnforcements(Class<T> clazz) {
        List<T> result = new ArrayList<T>();
        for (LicenseEnforcement le : licenseEnforcements) {
            if (clazz.isAssignableFrom(le.getClass()))
                result.add((T) le);
        }
        return result;
    }

    static {
        // Add default definitions
        defaultDefinitions.add(new DefaultPagesDefinition() {
            @Override
            public String getLoginPageUri(HttpServletRequest request, HttpServletResponse response) {
                return "/login.htm";
            }

            @Override
            public String getLoggedInPageUri(HttpServletRequest request, HttpServletResponse response, User user) {
                return "/data_point_details.shtm";
            }

            @Override
            public String getFirstUserLoginPageUri(HttpServletRequest request, HttpServletResponse response, User user) {
                return "/help.shtm";
            }
        });
    }
}
