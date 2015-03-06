/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.NotImplementedException;
import com.serotonin.m2m2.module.MenuItemDefinition.Visibility;
import com.serotonin.m2m2.module.UriMappingDefinition.Permission;
import com.serotonin.m2m2.module.license.LicenseEnforcement;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.template.DataPointPropertiesTemplateDefinition;
import com.serotonin.m2m2.web.mvc.UrlHandler;
import com.serotonin.m2m2.web.mvc.controller.DataPointDetailsController;
import com.serotonin.m2m2.web.mvc.controller.FileUploadController;
import com.serotonin.m2m2.web.mvc.controller.ModulesController;

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
    private static Map<String, TemplateDefinition> TEMPLATE_DEFINITIONS;
    private static Map<String, ModelDefinition> MODEL_DEFINITIONS;

    private static Map<MenuItemDefinition.Visibility, List<MenuItemDefinition>> MENU_ITEMS;

    private static final List<LicenseEnforcement> licenseEnforcements = new ArrayList<LicenseEnforcement>();
    private static final List<ModuleElementDefinition> preDefaults = new ArrayList<ModuleElementDefinition>();
    private static final List<ModuleElementDefinition> postDefaults = new ArrayList<ModuleElementDefinition>();

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
                    Map<String, DataSourceDefinition> map = new HashMap<String, DataSourceDefinition>();
                    for (Module module : MODULES.values()) {
                        for (DataSourceDefinition def : module.getDefinitions(DataSourceDefinition.class))
                            map.put(def.getDataSourceTypeName(), def);
                    }
                    DATA_SOURCE_DEFINITIONS = map;
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
                    Map<String, PublisherDefinition> map = new HashMap<String, PublisherDefinition>();
                    for (Module module : MODULES.values()) {
                        for (PublisherDefinition def : module.getDefinitions(PublisherDefinition.class))
                            map.put(def.getPublisherTypeName(), def);
                    }
                    PUBLISHER_DEFINITIONS = map;
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
                    Map<String, SystemEventTypeDefinition> map = new HashMap<String, SystemEventTypeDefinition>();
                    for (Module module : MODULES.values()) {
                        for (SystemEventTypeDefinition def : module.getDefinitions(SystemEventTypeDefinition.class))
                            map.put(def.getTypeName(), def);
                    }
                    SYSTEM_EVENT_TYPE_DEFINITIONS = map;
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
                    Map<String, EventTypeDefinition> map = new HashMap<String, EventTypeDefinition>();
                    for (Module module : MODULES.values()) {
                        for (EventTypeDefinition def : module.getDefinitions(EventTypeDefinition.class))
                            map.put(def.getTypeName(), def);
                    }
                    EVENT_TYPE_DEFINITIONS = map;
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
                    Map<String, AuditEventTypeDefinition> map = new HashMap<String, AuditEventTypeDefinition>();
                    for (Module module : MODULES.values()) {
                        for (AuditEventTypeDefinition def : module.getDefinitions(AuditEventTypeDefinition.class))
                            map.put(def.getTypeName(), def);
                    }
                    AUDIT_EVENT_TYPE_DEFINITIONS = map;
                }
            }
        }
    }
    
    //
    //
    // Template special handling
    //
    public static TemplateDefinition getTemplateDefinition(String type) {
        ensureTemplateDefinitions();
        return TEMPLATE_DEFINITIONS.get(type);
    }

    public static Set<String> getTemplateDefinitionTypes() {
        ensureTemplateDefinitions();
        return TEMPLATE_DEFINITIONS.keySet();
    }

    private static void ensureTemplateDefinitions() {
        if (TEMPLATE_DEFINITIONS == null) {
            synchronized (LOCK) {
                if (TEMPLATE_DEFINITIONS == null) {
                    Map<String, TemplateDefinition> map = new HashMap<String, TemplateDefinition>();
                    //Add in the Core Types
                    DataPointPropertiesTemplateDefinition dppDef = new DataPointPropertiesTemplateDefinition();
                    map.put(dppDef.getTemplateTypeName(), dppDef);
                    for (Module module : MODULES.values()) {
                        for (TemplateDefinition def : module.getDefinitions(TemplateDefinition.class))
                            map.put(def.getTemplateTypeName(), def);
                    }
                    TEMPLATE_DEFINITIONS = map;
                }
            }
        }
    }
    
    //
    //
    // Template special handling
    //
    public static ModelDefinition getModelDefinition(String type) {
        ensureModelDefinitions();
        return MODEL_DEFINITIONS.get(type);
    }

    public static Set<String> getModelDefinitionTypes() {
        ensureModelDefinitions();
        return MODEL_DEFINITIONS.keySet();
    }

    private static void ensureModelDefinitions() {
        if (MODEL_DEFINITIONS == null) {
            synchronized (LOCK) {
                if (MODEL_DEFINITIONS == null) {
                    Map<String, ModelDefinition> map = new HashMap<String, ModelDefinition>();
                    //Add in the Core Types
                    //TODO Data Point Properties Template DEF

                    for (Module module : MODULES.values()) {
                        for (ModelDefinition def : module.getDefinitions(ModelDefinition.class))
                            map.put(def.getModelTypeName(), def);
                    }
                    MODEL_DEFINITIONS = map;
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
        defs.addAll(Module.getDefinitions(preDefaults, clazz));
        for (Module module : MODULES.values())
            defs.addAll(module.getDefinitions(clazz));
        defs.addAll(Module.getDefinitions(postDefaults, clazz));
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
    public static Map<MenuItemDefinition.Visibility, List<MenuItemDefinition>> getMenuItems() {
        if (MENU_ITEMS == null) {
            synchronized (LOCK) {
                if (MENU_ITEMS == null) {
                    Map<MenuItemDefinition.Visibility, List<MenuItemDefinition>> map = new HashMap<MenuItemDefinition.Visibility, List<MenuItemDefinition>>();

                    for (MenuItemDefinition mi : getDefinitions(MenuItemDefinition.class)) {
                        boolean add = true;
                        // Special handling of url mapping definitions
                        if (mi instanceof UrlMappingDefinition)
                            add = !StringUtils.isBlank(((UrlMappingDefinition) mi).getMenuKey());

                        if (add) {
                            List<MenuItemDefinition> permList = map.get(mi.getVisibility());

                            if (permList == null) {
                                permList = new ArrayList<MenuItemDefinition>();
                                map.put(mi.getVisibility(), permList);
                            }

                            permList.add(mi);
                        }
                    }

                    for (List<MenuItemDefinition> list : map.values()) {
                        Collections.sort(list, new Comparator<MenuItemDefinition>() {
                            @Override
                            public int compare(MenuItemDefinition m1, MenuItemDefinition m2) {
                                return m1.getOrder() - m2.getOrder();
                            }
                        });
                    }

                    MENU_ITEMS = map;
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
        postDefaults.add(new DefaultPagesDefinition() {
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

            @Override
            public String getUnauthorizedPageUri(HttpServletRequest request, HttpServletResponse response, User user) {
                return "/unauthorized.htm";
            }
        });

        preDefaults.add(createMenuItemDefinition("pointDetailsMi", Visibility.USER, "header.dataPoints", "icon_comp",
                "/data_point_details.shtm"));
        preDefaults.add(createMenuItemDefinition("eventsMi", Visibility.USER, "header.alarms", "flag_white",
                "/events.shtm"));

        preDefaults.add(createMenuItemDefinition("eventHandlersMi", Visibility.DATA_SOURCE, "header.eventHandlers",
                "cog", "/event_handlers.shtm"));
        
        preDefaults.add(createMenuItemDefinition("dataSourcesMi", Visibility.DATA_SOURCE, "header.dataSources",
                "icon_ds", "/data_sources.shtm"));

        preDefaults.add(createMenuItemDefinition("pointHierarchyMi", Visibility.ADMINISTRATOR, "header.pointHierarchy",
                "folder_brick", "/point_hierarchy.shtm"));
        preDefaults.add(createMenuItemDefinition("mailingListsMi", Visibility.ADMINISTRATOR, "header.mailingLists",
                "book", "/mailing_lists.shtm"));
        preDefaults.add(createMenuItemDefinition("publishersMi", Visibility.ADMINISTRATOR, "header.publishers",
                "transmit", "/publishers.shtm"));
        preDefaults.add(createMenuItemDefinition("systemSettingsMi", Visibility.ADMINISTRATOR, "header.systemSettings",
                "application_form", "/system_settings.shtm"));
        preDefaults.add(createMenuItemDefinition("modulesMi", Visibility.ADMINISTRATOR, "header.modules", "puzzle",
                "/modules.shtm"));
        preDefaults.add(createMenuItemDefinition("emportMi", Visibility.ADMINISTRATOR, "header.emport", "emport",
                "/emport.shtm"));

        preDefaults.add(createUriMappingDefinition(Permission.USER, "/data_point_details.shtm",
                new DataPointDetailsController(), "/WEB-INF/jsp/dataPointDetails.jsp"));
        
        //Mappings for Event Report and Legacy Alarms Page
        preDefaults.add(createUriMappingDefinition(Permission.USER, "/events.shtm", null, "/WEB-INF/jsp/eventsReport.jsp"));
        preDefaults.add(createUriMappingDefinition(Permission.USER, "/pending_alarms.shtm", null, "/WEB-INF/jsp/events.jsp"));
        
        preDefaults.add(createUriMappingDefinition(Permission.DATA_SOURCE, "/event_handlers.shtm", null,
                "/WEB-INF/jsp/eventHandlers.jsp"));
        preDefaults.add(createUriMappingDefinition(Permission.DATA_SOURCE, "/data_sources.shtm", null,
                "/WEB-INF/jsp/dataSource.jsp"));
        preDefaults.add(createUriMappingDefinition(Permission.ADMINISTRATOR, "/point_hierarchy.shtm", null,
                "/WEB-INF/jsp/pointHierarchy.jsp"));
        preDefaults.add(createUriMappingDefinition(Permission.ADMINISTRATOR, "/mailing_lists.shtm", null,
                "/WEB-INF/jsp/mailingLists.jsp"));
        preDefaults.add(createUriMappingDefinition(Permission.ADMINISTRATOR, "/publishers.shtm", null,
                "/WEB-INF/jsp/publisherList.jsp"));
        preDefaults.add(createUriMappingDefinition(Permission.ADMINISTRATOR, "/system_settings.shtm", null,
                "/WEB-INF/jsp/systemSettings.jsp"));
        preDefaults.add(createUriMappingDefinition(Permission.ADMINISTRATOR, "/modules.shtm", new ModulesController(),
                "/WEB-INF/jsp/modules.jsp"));
        preDefaults.add(createUriMappingDefinition(Permission.ADMINISTRATOR, "/emport.shtm", null,
                "/WEB-INF/jsp/emport.jsp"));        
        
        //Demo for Rest API
        preDefaults.add(createUriMappingDefinition(Permission.USER, "/rest.shtm", null, "/WEB-INF/jsp/rest.jsp"));
        
        /* Emport Mappings */
        preDefaults.add(createUriMappingDefinition(Permission.DATA_SOURCE, "/upload.shtm", new FileUploadController(),
                "none.jsp"));
        
        /* MOBILE MAPPINGS */
        preDefaults.add(createUriMappingDefinition(Permission.USER, "/mobile_data_point_details.shtm",
                new DataPointDetailsController(), "/WEB-INF/jsp/mobile/dataPointDetails.jsp"));

        /* Startup/Shutdown Mappings */
        //Defined in springDispatcher servlet for now
        //preDefaults.add(createUriMappingDefinition(Permission.ANONYMOUS, "/startup.htm", null, "/WEB-INF/jsp/starting.jsp"));
        //preDefaults.add(createUriMappingDefinition(Permission.ANONYMOUS, "/shutdown.htm", null, "/WEB-INF/jsp/shutdown.jsp"));
        
        preDefaults.add(createMenuItemDefinition("helpMi", Visibility.ANONYMOUS, "header.help", "help", "/help.shtm"));
    }

    static MenuItemDefinition createMenuItemDefinition(final String id, final Visibility visibility,
            final String textKey, final String png, final String href) {
        return new MenuItemDefinition() {
            @Override
            public Visibility getVisibility() {
                return visibility;
            }

            @Override
            public String getId(HttpServletRequest request, HttpServletResponse response) {
                return id;
            }

            @Override
            public String getTextKey(HttpServletRequest request, HttpServletResponse response) {
                return textKey;
            }

            @Override
            public String getImagePath(HttpServletRequest request, HttpServletResponse response) {
                return "/images/" + png + ".png";
            }

            @Override
            public String getImage(HttpServletRequest request, HttpServletResponse response) {
                throw new NotImplementedException();
            }

            @Override
            public String getHref(HttpServletRequest request, HttpServletResponse response) {
                return href;
            }
        };
    }

    static UriMappingDefinition createUriMappingDefinition(final Permission permission, final String path,
            final UrlHandler handler, final String jspPath) {
        return new UriMappingDefinition() {
            @Override
            public Permission getPermission() {
                return permission;
            }

            @Override
            public String getPath() {
                return path;
            }

            @Override
            public UrlHandler getHandler() {
                return handler;
            }

            @Override
            public String getJspPath() {
                return jspPath;
            }
        };
    }

}
