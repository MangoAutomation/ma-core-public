/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.module;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.OrderComparator;

import com.github.zafarkhaja.semver.Version;
import com.infiniteautomation.mango.spring.service.FileStoreService;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.ICoreLicense;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.definitions.dataPoint.DataPointChangeDefinition;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.event.detector.AbstractEventDetectorVO;
import com.serotonin.provider.Providers;

/**
 * The registry of all modules in an MA instance.
 *
 * @author Matthew Lohbihler
 */
public class ModuleRegistry {
    static final Log LOG = LogFactory.getLog(ModuleRegistry.class);

    public static final String CORE_MODULE_NAME = "core";

    private static final Object LOCK = new Object();
    private static final Map<String, Module> MODULES = new LinkedHashMap<String, Module>();
    private static final Map<String, String> MISSING_DEPENDENCIES = new LinkedHashMap<>();
    private static final List<Module> UNLOADED_MODULES = new CopyOnWriteArrayList<>();

    private static Map<String, DataSourceDefinition<?>> DATA_SOURCE_DEFINITIONS;
    private static Map<String, PublisherDefinition<?>> PUBLISHER_DEFINITIONS;
    private static Map<String, EventTypeDefinition> EVENT_TYPE_DEFINITIONS;
    private static Map<String, SystemEventTypeDefinition> SYSTEM_EVENT_TYPE_DEFINITIONS;
    private static Map<String, AuditEventTypeDefinition> AUDIT_EVENT_TYPE_DEFINITIONS;
    private static Map<String, EventHandlerDefinition<?>> EVENT_HANDLER_DEFINITIONS;
    private static Map<String, EventDetectorDefinition<? extends AbstractEventDetectorVO>> EVENT_DETECTOR_DEFINITIONS;
    private static Map<String, PermissionDefinition> PERMISSION_DEFINITIONS;

    private static List<AngularJSModuleDefinition> ANGULARJS_MODULE_DEFINITIONS;
    private static List<SystemSettingsDefinition> SYSTEM_SETTINGS_DEFINITIONS;
    private static Map<String, SystemInfoDefinition<?>> SYSTEM_INFO_DEFINITIONS;
    private static List<SystemSettingsListenerDefinition> SYSTEM_SETTINGS_LISTENER_DEFINITIONS;
    private static Map<String, SystemActionDefinition> SYSTEM_ACTION_DEFINITIONS;
    private static Map<String, FileStoreDefinition> FILE_STORE_DEFINITIONS;
    private static List<MangoJavascriptContextObjectDefinition> JAVASCRIPT_CONTEXT_DEFINITIONS;
    private static List<DataPointChangeDefinition> DATA_POINT_CHANGE_DEFINITIONS;

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

    /**
     * Modules that could not be loaded are added here, only add one at a time
     * @param module
     * @return
     */
    public static void addUnloadedModule(Module module){
        if(!UNLOADED_MODULES.contains(module))
            UNLOADED_MODULES.add(module);
    }

    public static List<Module> getUnloadedModules(){
        return UNLOADED_MODULES;
    }

    /**
     * Add the missing dependency
     * @param moduleName
     * @param version
     */
    public static void addMissingDependency(String moduleName, String version){
        MISSING_DEPENDENCIES.put(moduleName, version);
    }

    /**
     * List all missing dependencies
     *
     * Users must not modify this list.
     * @return
     */
    public static Map<String, String> getMissingDependencies(){
        return MISSING_DEPENDENCIES;
    }

    //
    //
    // Data source special handling
    //
    @SuppressWarnings("unchecked")
    public static <T extends DataSourceVO> DataSourceDefinition<T> getDataSourceDefinition(String type) {
        ensureDataSourceDefinitions();
        return (DataSourceDefinition<T>) DATA_SOURCE_DEFINITIONS.get(type);
    }

    public static Set<String> getDataSourceDefinitionTypes() {
        ensureDataSourceDefinitions();
        return DATA_SOURCE_DEFINITIONS.keySet();
    }

    private static void ensureDataSourceDefinitions() {
        if (DATA_SOURCE_DEFINITIONS == null) {
            synchronized (LOCK) {
                if (DATA_SOURCE_DEFINITIONS == null) {
                    Map<String, DataSourceDefinition<?>> map = new HashMap<String, DataSourceDefinition<?>>();
                    for (Module module : MODULES.values()) {
                        for (DataSourceDefinition<?> def : module.getDefinitions(DataSourceDefinition.class))
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
    public static PublisherDefinition<?> getPublisherDefinition(String type) {
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
                    Map<String, PublisherDefinition<?>> map = new HashMap<String, PublisherDefinition<?>>();
                    for (Module module : MODULES.values()) {
                        for (PublisherDefinition<?> def : module.getDefinitions(PublisherDefinition.class))
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
    public static List<EventTypeDefinition> getEventTypeDefinitions() {
        ensureEventTypeDefinitions();
        return new ArrayList<>(EVENT_TYPE_DEFINITIONS.values());
    }
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
    // Event Handler special handling
    //
    public static EventHandlerDefinition<?> getEventHandlerDefinition(String type) {
        ensureEventHandlerDefinitions();
        return EVENT_HANDLER_DEFINITIONS.get(type);
    }

    public static List<EventHandlerDefinition<?>> getEventHandlerDefinitions(){
        ensureEventHandlerDefinitions();
        return new ArrayList<EventHandlerDefinition<?>>(EVENT_HANDLER_DEFINITIONS.values());
    }

    public static Set<String> getEventHandlerDefinitionTypes() {
        ensureEventHandlerDefinitions();
        return EVENT_HANDLER_DEFINITIONS.keySet();
    }

    private static void ensureEventHandlerDefinitions() {
        if (EVENT_HANDLER_DEFINITIONS == null) {
            synchronized (LOCK) {
                if (EVENT_HANDLER_DEFINITIONS == null) {
                    Map<String, EventHandlerDefinition<?>> map = new HashMap<String, EventHandlerDefinition<?>>();
                    for (Module module : MODULES.values()) {
                        for (EventHandlerDefinition<?> def : module.getDefinitions(EventHandlerDefinition.class))
                            map.put(def.getEventHandlerTypeName(), def);
                    }
                    EVENT_HANDLER_DEFINITIONS = map;
                }
            }
        }
    }

    //
    //
    // Event Detector special handling
    //

    /**
     * Get based on definition type name
     * @param type
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T extends EventDetectorDefinition<?>> T getEventDetectorDefinition(String type) {
        ensureEventDetectorDefinitions();
        return (T) EVENT_DETECTOR_DEFINITIONS.get(type);
    }

    @SuppressWarnings("unchecked")
    public static <T extends EventDetectorDefinition<?>> List<T> getEventDetectorDefinitionsBySourceType(String sourceType) {
        ensureEventDetectorDefinitions();
        List<T> matching = new ArrayList<>();
        for(EventDetectorDefinition<?> definition : EVENT_DETECTOR_DEFINITIONS.values()) {
            if(StringUtils.equals(definition.getSourceTypeName(), sourceType))
                matching.add((T)definition);
        }
        return matching;
    }

    public static List<EventDetectorDefinition<?>> getEventDetectorDefinitions(){
        ensureEventDetectorDefinitions();
        return new ArrayList<EventDetectorDefinition<?>>(EVENT_DETECTOR_DEFINITIONS.values());
    }

    public static Set<String> getEventDetectorDefinitionTypes() {
        ensureEventDetectorDefinitions();
        return EVENT_DETECTOR_DEFINITIONS.keySet();
    }

    private static void ensureEventDetectorDefinitions() {
        if (EVENT_DETECTOR_DEFINITIONS == null) {
            synchronized (LOCK) {
                if (EVENT_DETECTOR_DEFINITIONS == null) {
                    Map<String, EventDetectorDefinition<? extends AbstractEventDetectorVO>> map = new HashMap<>();
                    for (Module module : MODULES.values()) {
                        for (EventDetectorDefinition<?> def : module.getDefinitions(EventDetectorDefinition.class))
                            map.put(def.getEventDetectorTypeName(), def);
                    }
                    EVENT_DETECTOR_DEFINITIONS = map;
                }
            }
        }
    }

    //
    //
    // Permission Definitions special handling
    //
    public static Map<String, PermissionDefinition> getPermissionDefinitions() {
        ensurePermissionDefinitions();
        return new HashMap<String, PermissionDefinition>(PERMISSION_DEFINITIONS);
    }

    public static PermissionDefinition getPermissionDefinition(String key){
        ensurePermissionDefinitions();
        return PERMISSION_DEFINITIONS.get(key);
    }

    private static void ensurePermissionDefinitions() {
        if (PERMISSION_DEFINITIONS == null) {
            synchronized (LOCK) {
                if (PERMISSION_DEFINITIONS == null) {
                    Map<String, PermissionDefinition> map = new HashMap<String, PermissionDefinition>();
                    for (Module module : MODULES.values()) {
                        for (PermissionDefinition def : module.getDefinitions(PermissionDefinition.class))
                            map.put(def.getPermissionTypeName(), def);
                    }
                    PERMISSION_DEFINITIONS = map;
                }
            }
        }
    }

    //
    //
    // AngularJS Module special handling
    //
    public static List<AngularJSModuleDefinition> getAngularJSDefinitions() {
        ensureAngularJSModuleDefinitions();
        return ANGULARJS_MODULE_DEFINITIONS;
    }

    private static void ensureAngularJSModuleDefinitions() {
        if (ANGULARJS_MODULE_DEFINITIONS == null) {
            synchronized (LOCK) {
                if (ANGULARJS_MODULE_DEFINITIONS == null) {
                    ANGULARJS_MODULE_DEFINITIONS = getDefinitions(AngularJSModuleDefinition.class);
                }
            }
        }
    }

    //
    //
    // System Settings special handling
    //
    public static List<SystemSettingsDefinition> getSystemSettingsDefinitions() {
        ensureSystemSettingsDefinitions();
        return SYSTEM_SETTINGS_DEFINITIONS;
    }

    private static void ensureSystemSettingsDefinitions() {
        if (SYSTEM_SETTINGS_DEFINITIONS == null) {
            synchronized (LOCK) {
                if (SYSTEM_SETTINGS_DEFINITIONS == null) {
                    SYSTEM_SETTINGS_DEFINITIONS = getDefinitions(SystemSettingsDefinition.class);
                }
            }
        }
    }

    //
    //
    // Read Only Settings special handling
    //
    public static List<SystemSettingsListenerDefinition> getSystemSettingListenerDefinitions() {
        ensureSystemSettingsListenerDefinitions();
        return SYSTEM_SETTINGS_LISTENER_DEFINITIONS;
    }

    private static void ensureSystemSettingsListenerDefinitions() {
        if (SYSTEM_SETTINGS_LISTENER_DEFINITIONS == null) {
            synchronized (LOCK) {
                if (SYSTEM_SETTINGS_LISTENER_DEFINITIONS == null) {
                    SYSTEM_SETTINGS_LISTENER_DEFINITIONS = getDefinitions(SystemSettingsListenerDefinition.class);
                }
            }
        }
    }

    //
    //
    // System Info Settings special handling
    //
    public static Map<String, SystemInfoDefinition<?>> getSystemInfoDefinitions() {
        ensureSystemInfoDefinitions();
        return new HashMap<String, SystemInfoDefinition<?>>(SYSTEM_INFO_DEFINITIONS);
    }

    public static SystemInfoDefinition<?> getSystemInfoDefinition(String key){
        ensureSystemInfoDefinitions();
        return SYSTEM_INFO_DEFINITIONS.get(key);
    }

    private static void ensureSystemInfoDefinitions() {
        if (SYSTEM_INFO_DEFINITIONS == null) {
            synchronized (LOCK) {
                if (SYSTEM_INFO_DEFINITIONS == null) {
                    Map<String, SystemInfoDefinition<?>> map = new HashMap<String, SystemInfoDefinition<?>>();
                    for (Module module : MODULES.values()) {
                        for (SystemInfoDefinition<?> def : module.getDefinitions(SystemInfoDefinition.class))
                            map.put(def.getKey(), def);
                    }
                    SYSTEM_INFO_DEFINITIONS = map;
                }
            }
        }
    }

    //
    //
    // System Action Settings special handling
    //
    public static Map<String, SystemActionDefinition> getSystemActionDefinitions() {
        ensureSystemActionDefinitions();
        return new HashMap<String, SystemActionDefinition>(SYSTEM_ACTION_DEFINITIONS);
    }

    public static SystemActionDefinition getSystemActionDefinition(String key){
        ensureSystemActionDefinitions();
        return SYSTEM_ACTION_DEFINITIONS.get(key);
    }

    private static void ensureSystemActionDefinitions() {
        if (SYSTEM_ACTION_DEFINITIONS == null) {
            synchronized (LOCK) {
                if (SYSTEM_ACTION_DEFINITIONS == null) {
                    Map<String, SystemActionDefinition> map = new HashMap<>();
                    for (Module module : MODULES.values()) {
                        for (SystemActionDefinition def : module.getDefinitions(SystemActionDefinition.class))
                            map.put(def.getKey(), def);
                    }
                    SYSTEM_ACTION_DEFINITIONS = map;
                }
            }
        }
    }

    //
    //
    // File Store special handling
    //
    public static Map<String, FileStoreDefinition> getFileStoreDefinitions() {
        ensureFileStoreDefinitions();
        return new HashMap<String, FileStoreDefinition>(FILE_STORE_DEFINITIONS);
    }

    /**
     * This function returns a FileStoreDefinition if it exists in a module element definition.
     *  It is preferred to go through the FileStoreDao.getInstance().getfileStoreDefinition method
     *  as that will also resolve any user defined filestores with that file store name. This is
     *  an acceptable method to call in a module if the module doesn't permit use of custom filestores.
     * @param name
     * @return
     */
    public static FileStoreDefinition getFileStoreDefinition(String name){
        ensureFileStoreDefinitions();
        return FILE_STORE_DEFINITIONS.get(name);
    }

    private static void ensureFileStoreDefinitions() {
        if (FILE_STORE_DEFINITIONS == null) {
            synchronized (LOCK) {
                if (FILE_STORE_DEFINITIONS == null) {
                    Map<String, FileStoreDefinition> map = new HashMap<>();
                    for (Module module : MODULES.values()) {
                        for (FileStoreDefinition def : module.getDefinitions(FileStoreDefinition.class)) {
                            try {
                                def.ensureExists();
                            } catch (IOException e) {
                                LOG.error("Couldn't create directory for file store", e);
                            }

                            String xid = def.getStoreName();
                            if (FileStoreService.INVALID_XID_CHARACTERS.matcher(xid).find()) {
                                throw new RuntimeException("Filestore name contains invalid character");
                            }
                            map.put(xid, def);
                        }
                    }
                    FILE_STORE_DEFINITIONS = map;
                }
            }
        }
    }

    //
    //
    // Read Only Settings special handling
    //
    public static List<MangoJavascriptContextObjectDefinition> getMangoJavascriptContextObjectDefinitions() {
        ensureJavascriptContextDefinitions();
        return JAVASCRIPT_CONTEXT_DEFINITIONS;
    }

    private static void ensureJavascriptContextDefinitions() {
        if (JAVASCRIPT_CONTEXT_DEFINITIONS == null) {
            synchronized (LOCK) {
                if (JAVASCRIPT_CONTEXT_DEFINITIONS == null) {
                    JAVASCRIPT_CONTEXT_DEFINITIONS = getDefinitions(MangoJavascriptContextObjectDefinition.class);
                }
            }
        }
    }

    //
    //
    // Read Only Settings special handling
    //
    public static List<DataPointChangeDefinition> getDataPointChangeDefinitions() {
        ensureDataPointChangeDefinitions();
        return DATA_POINT_CHANGE_DEFINITIONS;
    }

    private static void ensureDataPointChangeDefinitions() {
        if (DATA_POINT_CHANGE_DEFINITIONS == null) {
            synchronized (LOCK) {
                if (DATA_POINT_CHANGE_DEFINITIONS == null) {
                    DATA_POINT_CHANGE_DEFINITIONS = getDefinitions(DataPointChangeDefinition.class);
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
        defs.sort(OrderComparator.INSTANCE);
        return defs;
    }

    public static <T extends ModuleElementDefinition> T getDefinition(Class<T> clazz) {
        return getDefinition(clazz, true);
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

    public static final CoreModule CORE_MODULE = new CoreModule(ModuleRegistry.CORE_MODULE_NAME,
            Common.getVersion(),
            new TranslatableMessage("modules.core.description"),
            "Infinite Automation Systems, Inc.",
            "https://www.infiniteautomation.com",
            null, -1, Common.isCoreSigned());

    /**
     * Class marker for special core module
     *
     * @author Terry Packer
     */
    public static class CoreModule extends Module {

        public CoreModule(String name, Version version, TranslatableMessage description,
                String vendor, String vendorUrl, String dependencies, int loadOrder,
                boolean signed) {
            super(name, version, description, vendor, vendorUrl, dependencies, loadOrder, signed);

            loadDefinitions(CoreModule.class.getClassLoader());
            addDefinition((LicenseDefinition) Providers.get(ICoreLicense.class));
            addModule(this);
        }

        @Override
        public String getLicenseType() {
            if(Common.isInvalid()) {
                return "Invalid";
            }else {
                return Common.license() == null ? null : Common.license().getLicenseType();
            }
        }
    }
}
