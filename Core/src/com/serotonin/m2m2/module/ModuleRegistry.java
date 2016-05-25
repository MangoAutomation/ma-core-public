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
import org.springframework.web.servlet.mvc.Controller;

import com.serotonin.NotImplementedException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.module.MenuItemDefinition.Visibility;
import com.serotonin.m2m2.module.UriMappingDefinition.Permission;
import com.serotonin.m2m2.module.definitions.event.detectors.AlphanumericRegexStateEventDetectorDefinition;
import com.serotonin.m2m2.module.definitions.event.detectors.AlphanumericStateEventDetectorDefinition;
import com.serotonin.m2m2.module.definitions.event.detectors.AnalogChangeEventDetectorDefinition;
import com.serotonin.m2m2.module.definitions.event.detectors.AnalogHighLimitEventDetectorDefinition;
import com.serotonin.m2m2.module.definitions.event.detectors.AnalogLowLimitEventDetectorDefinition;
import com.serotonin.m2m2.module.definitions.event.detectors.AnalogRangeEventDetectorDefinition;
import com.serotonin.m2m2.module.definitions.event.detectors.BinaryStateEventDetectorDefinition;
import com.serotonin.m2m2.module.definitions.event.detectors.MultistateStateEventDetectorDefinition;
import com.serotonin.m2m2.module.definitions.event.detectors.NegativeCusumEventDetectorDefinition;
import com.serotonin.m2m2.module.definitions.event.detectors.NoChangeEventDetectorDefinition;
import com.serotonin.m2m2.module.definitions.event.detectors.NoUpdateEventDetectorDefinition;
import com.serotonin.m2m2.module.definitions.event.detectors.PointChangeEventDetectorDefinition;
import com.serotonin.m2m2.module.definitions.event.detectors.PositiveCusumEventDetectorDefinition;
import com.serotonin.m2m2.module.definitions.event.detectors.SmoothnessEventDetectorDefinition;
import com.serotonin.m2m2.module.definitions.event.detectors.StateChangeCountEventDetectorDefinition;
import com.serotonin.m2m2.module.definitions.event.handlers.EmailEventHandlerDefinition;
import com.serotonin.m2m2.module.definitions.event.handlers.ProcessEventHandlerDefinition;
import com.serotonin.m2m2.module.definitions.event.handlers.SetPointEventHandlerDefinition;
import com.serotonin.m2m2.module.definitions.permissions.EventsViewPermissionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.LegacyPointDetailsViewPermissionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.UsersViewPermissionDefinition;
import com.serotonin.m2m2.module.definitions.websocket.AuditEventWebSocketDefinition;
import com.serotonin.m2m2.module.definitions.websocket.DataPointWebSocketDefinition;
import com.serotonin.m2m2.module.definitions.websocket.DataSourceWebSocketDefinition;
import com.serotonin.m2m2.module.definitions.websocket.EventDetectorWebSocketDefinition;
import com.serotonin.m2m2.module.definitions.websocket.EventHandlerWebSocketDefinition;
import com.serotonin.m2m2.module.definitions.websocket.EventInstanceWebSocketDefinition;
import com.serotonin.m2m2.module.definitions.websocket.JsonDataWebSocketDefinition;
import com.serotonin.m2m2.module.definitions.websocket.TemplateWebSocketDefinition;
import com.serotonin.m2m2.module.definitions.websocket.UserCommentWebSocketDefinition;
import com.serotonin.m2m2.module.definitions.websocket.UserWebSocketDefinition;
import com.serotonin.m2m2.module.license.LicenseEnforcement;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.Permissions;
import com.serotonin.m2m2.vo.template.DataPointPropertiesTemplateDefinition;
import com.serotonin.m2m2.web.mvc.UrlHandler;
import com.serotonin.m2m2.web.mvc.controller.DataPointDetailsController;
import com.serotonin.m2m2.web.mvc.controller.DataPointEditController;
import com.serotonin.m2m2.web.mvc.controller.DataSourceEditController;
import com.serotonin.m2m2.web.mvc.controller.DataSourcePropertiesController;
import com.serotonin.m2m2.web.mvc.controller.FileUploadController;
import com.serotonin.m2m2.web.mvc.controller.HelpController;
import com.serotonin.m2m2.web.mvc.controller.LogoutController;
import com.serotonin.m2m2.web.mvc.controller.ModulesController;
import com.serotonin.m2m2.web.mvc.controller.PublisherEditController;
import com.serotonin.m2m2.web.mvc.controller.ShutdownController;
import com.serotonin.m2m2.web.mvc.controller.StartupController;
import com.serotonin.m2m2.web.mvc.controller.UnauthorizedController;
import com.serotonin.m2m2.web.mvc.controller.UsersController;
import com.serotonin.m2m2.web.mvc.rest.v1.model.RestErrorModelDefinition;
import com.serotonin.m2m2.web.mvc.rest.v1.model.audit.AuditEventInstanceModelDefinition;
import com.serotonin.m2m2.web.mvc.rest.v1.model.dataPoint.DataPointModelDefinition;
import com.serotonin.m2m2.web.mvc.rest.v1.model.jsondata.JsonDataModelDefinition;

/**
 * The registry of all modules in an MA instance.
 * 
 * @author Matthew Lohbihler
 */
public class ModuleRegistry {
	
	public static final String SYSTEM_SETTINGS_URL = "/system_settings.shtm";
	
    private static final Object LOCK = new Object();
    private static final Map<String, Module> MODULES = new LinkedHashMap<String, Module>();

    private static Map<String, DataSourceDefinition> DATA_SOURCE_DEFINITIONS;
    private static Map<String, PublisherDefinition> PUBLISHER_DEFINITIONS;
    private static Map<String, EventTypeDefinition> EVENT_TYPE_DEFINITIONS;
    private static Map<String, SystemEventTypeDefinition> SYSTEM_EVENT_TYPE_DEFINITIONS;
    private static Map<String, AuditEventTypeDefinition> AUDIT_EVENT_TYPE_DEFINITIONS;
    private static Map<String, TemplateDefinition> TEMPLATE_DEFINITIONS;
    private static Map<String, ModelDefinition> MODEL_DEFINITIONS;
    private static Map<String, EventHandlerDefinition> EVENT_HANDLER_DEFINITIONS;
    private static Map<String, EventDetectorDefinition> EVENT_DETECTOR_DEFINITIONS;

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
                    for(TemplateDefinition def : Module.getDefinitions(preDefaults, TemplateDefinition.class)){
                    	map.put(def.getTemplateTypeName(), def);
                    }
                    for (Module module : MODULES.values()) {
                        for (TemplateDefinition def : module.getDefinitions(TemplateDefinition.class))
                            map.put(def.getTemplateTypeName(), def);
                    }
                    for(TemplateDefinition def : Module.getDefinitions(postDefaults, TemplateDefinition.class)){
                    	map.put(def.getTemplateTypeName(), def);
                    }
                    TEMPLATE_DEFINITIONS = map;
                }
            }
        }
    }
    
    //
    //
    // Model special handling
    //
    public static ModelDefinition getModelDefinition(String type) {
        ensureModelDefinitions();
        return MODEL_DEFINITIONS.get(type);
    }
    
    public static List<ModelDefinition> getModelDefinitions(){
        ensureModelDefinitions();
        return new ArrayList<ModelDefinition>(MODEL_DEFINITIONS.values());
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
                    for(ModelDefinition def : Module.getDefinitions(preDefaults, ModelDefinition.class)){
                    	map.put(def.getModelTypeName(), def);
                    }
                    for (Module module : MODULES.values()) {
                        for (ModelDefinition def : module.getDefinitions(ModelDefinition.class))
                            map.put(def.getModelTypeName(), def);
                    }
                    for(ModelDefinition def : Module.getDefinitions(postDefaults, ModelDefinition.class)){
                    	map.put(def.getModelTypeName(), def);
                    }
                    MODEL_DEFINITIONS = map;
                }
            }
        }
    }
    
    //
    //
    // Event Handler special handling
    //
    public static EventHandlerDefinition getEventHandlerDefinition(String type) {
        ensureEventHandlerDefinitions();
        return EVENT_HANDLER_DEFINITIONS.get(type);
    }
    
    public static List<EventHandlerDefinition> getEventHandlerDefinitions(){
    	ensureEventHandlerDefinitions();
        return new ArrayList<EventHandlerDefinition>(EVENT_HANDLER_DEFINITIONS.values());
    }

    public static Set<String> getEventHandlerDefinitionTypes() {
    	ensureEventHandlerDefinitions();
        return EVENT_HANDLER_DEFINITIONS.keySet();
    }

    private static void ensureEventHandlerDefinitions() {
        if (EVENT_HANDLER_DEFINITIONS == null) {
            synchronized (LOCK) {
                if (EVENT_HANDLER_DEFINITIONS == null) {
                    Map<String, EventHandlerDefinition> map = new HashMap<String, EventHandlerDefinition>();
                    for(EventHandlerDefinition def : Module.getDefinitions(preDefaults, EventHandlerDefinition.class)){
                    	map.put(def.getEventHandlerTypeName(), def);
                    }
                    for (Module module : MODULES.values()) {
                        for (EventHandlerDefinition def : module.getDefinitions(EventHandlerDefinition.class))
                            map.put(def.getEventHandlerTypeName(), def);
                    }
                    for(EventHandlerDefinition def : Module.getDefinitions(postDefaults, EventHandlerDefinition.class)){
                    	map.put(def.getEventHandlerTypeName(), def);
                    }
                    EVENT_HANDLER_DEFINITIONS = map;
                }
            }
        }
    }
    
    //
    //
    // Model special handling
    //
    public static EventDetectorDefinition getEventDetectorDefinition(String type) {
        ensureEventDetectorDefinitions();
        return EVENT_DETECTOR_DEFINITIONS.get(type);
    }
    
    public static List<EventDetectorDefinition> getEventDetectorDefinitions(){
    	ensureEventDetectorDefinitions();
        return new ArrayList<EventDetectorDefinition>(EVENT_DETECTOR_DEFINITIONS.values());
    }

    public static Set<String> getEventDetectorDefinitionTypes() {
    	ensureEventDetectorDefinitions();
        return EVENT_DETECTOR_DEFINITIONS.keySet();
    }

    private static void ensureEventDetectorDefinitions() {
        if (EVENT_DETECTOR_DEFINITIONS == null) {
            synchronized (LOCK) {
                if (EVENT_DETECTOR_DEFINITIONS == null) {
                    Map<String, EventDetectorDefinition> map = new HashMap<String, EventDetectorDefinition>();
                    for(EventDetectorDefinition def : Module.getDefinitions(preDefaults, EventDetectorDefinition.class)){
                    	map.put(def.getEventDetectorTypeName(), def);
                    }
                    for (Module module : MODULES.values()) {
                        for (EventDetectorDefinition def : module.getDefinitions(EventDetectorDefinition.class))
                            map.put(def.getEventDetectorTypeName(), def);
                    }
                    for(EventDetectorDefinition def : Module.getDefinitions(postDefaults, EventDetectorDefinition.class)){
                    	map.put(def.getEventDetectorTypeName(), def);
                    }
                    EVENT_DETECTOR_DEFINITIONS = map;
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
                return "/help.htm";
            }

            @Override
            public String getUnauthorizedPageUri(HttpServletRequest request, HttpServletResponse response, User user) {
                return "/unauthorized.htm";
            }
        });
        
        //Add in core Models
        preDefaults.add(new RestErrorModelDefinition());
        preDefaults.add(new JsonDataModelDefinition());
        preDefaults.add(new AuditEventInstanceModelDefinition());
        preDefaults.add(new DataPointModelDefinition());
        
        //TODO Add env property to load the Demo Swagger Endpoint then re-enable the demo controller
        //preDefaults.add(new DemoModelDefinition());
        
        //Add in core Web Sockets 
        preDefaults.add(new AuditEventWebSocketDefinition());
        preDefaults.add(new DataPointWebSocketDefinition());
        preDefaults.add(new DataSourceWebSocketDefinition());
        preDefaults.add(new EventDetectorWebSocketDefinition());
        preDefaults.add(new EventHandlerWebSocketDefinition());
        preDefaults.add(new EventInstanceWebSocketDefinition());
        preDefaults.add(new JsonDataWebSocketDefinition());
        preDefaults.add(new TemplateWebSocketDefinition());
        preDefaults.add(new UserCommentWebSocketDefinition());
        preDefaults.add(new UserWebSocketDefinition());

        //Add in the Core Templates
        preDefaults.add(new DataPointPropertiesTemplateDefinition());
        
        //Add in Core Event Handlers
        preDefaults.add(new EmailEventHandlerDefinition());
        preDefaults.add(new ProcessEventHandlerDefinition());
        preDefaults.add(new SetPointEventHandlerDefinition());
        
        //Add in Core Event Detectors
        preDefaults.add(new AlphanumericRegexStateEventDetectorDefinition());
        preDefaults.add(new AlphanumericStateEventDetectorDefinition());
        preDefaults.add(new AnalogChangeEventDetectorDefinition());
        preDefaults.add(new AnalogHighLimitEventDetectorDefinition());
        preDefaults.add(new AnalogLowLimitEventDetectorDefinition());
        preDefaults.add(new AnalogRangeEventDetectorDefinition());
        preDefaults.add(new BinaryStateEventDetectorDefinition());
        preDefaults.add(new MultistateStateEventDetectorDefinition());
        preDefaults.add(new NegativeCusumEventDetectorDefinition());
        preDefaults.add(new NoChangeEventDetectorDefinition());
        preDefaults.add(new PointChangeEventDetectorDefinition());
        preDefaults.add(new PositiveCusumEventDetectorDefinition());
        preDefaults.add(new SmoothnessEventDetectorDefinition());
        preDefaults.add(new StateChangeCountEventDetectorDefinition());
        preDefaults.add(new NoUpdateEventDetectorDefinition());
        
        preDefaults.add(new LegacyPointDetailsViewPermissionDefinition());
        preDefaults.add(createMenuItemDefinition("pointDetailsMi", Visibility.USER, "header.dataPoints", "icon_comp",
                "/data_point_details.shtm", LegacyPointDetailsViewPermissionDefinition.PERMISSION));
        
        preDefaults.add(new EventsViewPermissionDefinition());
        preDefaults.add(createMenuItemDefinition("eventsMi", Visibility.USER, "header.alarms", "flag_white",
                "/events.shtm", EventsViewPermissionDefinition.PERMISSION));

        preDefaults.add(new UsersViewPermissionDefinition());
        preDefaults.add(createMenuItemDefinition("usersMi", Visibility.USER, "header.users", "user",
                "/users.shtm", UsersViewPermissionDefinition.PERMISSION));

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
                "application_form", SYSTEM_SETTINGS_URL));
        preDefaults.add(createMenuItemDefinition("modulesMi", Visibility.ADMINISTRATOR, "header.modules", "puzzle",
                "/modules.shtm"));
        preDefaults.add(createMenuItemDefinition("emportMi", Visibility.ADMINISTRATOR, "header.emport", "emport",
                "/emport.shtm"));

        preDefaults.add(createUriMappingDefinition(Permission.CUSTOM, "/data_point_details.shtm",
                new DataPointDetailsController(), "/WEB-INF/jsp/dataPointDetails.jsp", LegacyPointDetailsViewPermissionDefinition.PERMISSION));
        
        //Mappings for Event Report and Legacy Alarms Page
        preDefaults.add(createUriMappingDefinition(Permission.CUSTOM, "/events.shtm", null, "/WEB-INF/jsp/eventsReport.jsp", EventsViewPermissionDefinition.PERMISSION));
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
                
        /* Emport Mappings */
        preDefaults.add(createUriMappingDefinition(Permission.DATA_SOURCE, "/upload.shtm", new FileUploadController(),
                null));
        
        /* MOBILE MAPPINGS */
        preDefaults.add(createUriMappingDefinition(Permission.USER, "/mobile_data_point_details.shtm",
                new DataPointDetailsController(), "/WEB-INF/jsp/mobile/dataPointDetails.jsp"));

        preDefaults.add(createMenuItemDefinition("helpMi", Visibility.ANONYMOUS, "header.help", "help", "/help.htm"));
    
        /* Controller Mappings */
        preDefaults.add(createControllerMappingDefinition(Permission.USER, "/data_point_edit.shtm", new DataPointEditController()));
        preDefaults.add(createControllerMappingDefinition(Permission.USER, "/data_source_properties.shtm", new DataSourcePropertiesController()));
        preDefaults.add(createControllerMappingDefinition(Permission.USER, "/data_source_edit.shtm", new DataSourceEditController()));
        preDefaults.add(createControllerMappingDefinition(Permission.USER, "/data_source_properties_error.shtm", new DataSourceEditController()));
        preDefaults.add(createControllerMappingDefinition(Permission.ANONYMOUS, "/help.htm", new HelpController()));
        preDefaults.add(createControllerMappingDefinition(Permission.ANONYMOUS, "/startup.htm", new StartupController()));
        preDefaults.add(createControllerMappingDefinition(Permission.ANONYMOUS, "/shutdown.htm", new ShutdownController()));
        preDefaults.add(createControllerMappingDefinition(Permission.ANONYMOUS, "/logout.htm", new LogoutController()));
        preDefaults.add(createControllerMappingDefinition(Permission.USER, "/publisher_edit.shtm", new PublisherEditController()));
        preDefaults.add(createControllerMappingDefinition(Permission.ANONYMOUS, "/unauthorized.htm", new UnauthorizedController()));
        preDefaults.add(createControllerMappingDefinition(Permission.CUSTOM, "/users.shtm", new UsersController(), UsersViewPermissionDefinition.PERMISSION));
        
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
  
    /**
     * Create with custom level permissions
     * @param id
     * @param visibility
     * @param textKey
     * @param png
     * @param href
     * @param permission
     * @return
     */
    static MenuItemDefinition createMenuItemDefinition(final String id, final Visibility visibility,
            final String textKey, final String png, final String href, final String permission) {
        return new MenuItemDefinition() {
            @Override
            public Visibility getVisibility() {
                return visibility;
            }
            
            @Override
            public boolean isVisible(HttpServletRequest request, HttpServletResponse response) {
            	return Permissions.hasPermission(Common.getUser(request), SystemSettingsDao.getValue(permission));
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

    /**
     * Create with custom permission level
     * @param level
     * @param path
     * @param handler
     * @param jspPath
     * @param permission
     * @return
     */
    static UriMappingDefinition createUriMappingDefinition(final Permission level, final String path,
            final UrlHandler handler, final String jspPath, final String permission) {
        return new UriMappingDefinition() {
            @Override
            public Permission getPermission() {
                return level;
            }

            @Override
            public boolean hasCustomPermission(User user){
            	return Permissions.hasPermission(user, SystemSettingsDao.getValue(permission));
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
    
    static ControllerMappingDefinition createControllerMappingDefinition(final Permission level, final String path,
            final Controller controller) {
        return new ControllerMappingDefinition() {
            @Override
            public Permission getPermission() {
                return level;
            }
            
            @Override
            public String getPath() {
                return path;
            }

            @Override
            public Controller getController() {
                return controller;
            }
        };
    }
    
    static ControllerMappingDefinition createControllerMappingDefinition(final Permission level, final String path,
            final Controller controller, final String permission) {
        return new ControllerMappingDefinition() {
            @Override
            public Permission getPermission() {
                return level;
            }
            
            @Override
            public String getPath() {
                return path;
            }

            @Override
            public Controller getController() {
                return controller;
            }
            /* (non-Javadoc)
             * @see com.serotonin.m2m2.module.ControllerMappingDefinition#hasCustomPermission(com.serotonin.m2m2.vo.User)
             */
            @Override
            public boolean hasCustomPermission(User user)
            		throws PermissionException {
            	return Permissions.hasPermission(user, SystemSettingsDao.getValue(permission));
            }
        };
    }
}
