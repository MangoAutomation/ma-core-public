/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.module;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.zafarkhaja.semver.Version;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.ICoreLicense;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.definitions.actions.ConfigurationBackupActionDefinition;
import com.serotonin.m2m2.module.definitions.actions.PurgeAllEventsActionDefinition;
import com.serotonin.m2m2.module.definitions.actions.PurgeAllPointValuesActionDefinition;
import com.serotonin.m2m2.module.definitions.actions.PurgeWithPurgeSettingsActionDefinition;
import com.serotonin.m2m2.module.definitions.actions.SqlBackupActionDefinition;
import com.serotonin.m2m2.module.definitions.actions.SqlRestoreActionDefinition;
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
import com.serotonin.m2m2.module.definitions.event.detectors.RateOfChangeDetectorDefinition;
import com.serotonin.m2m2.module.definitions.event.detectors.SmoothnessEventDetectorDefinition;
import com.serotonin.m2m2.module.definitions.event.detectors.StateChangeCountEventDetectorDefinition;
import com.serotonin.m2m2.module.definitions.event.handlers.EmailEventHandlerDefinition;
import com.serotonin.m2m2.module.definitions.event.handlers.ProcessEventHandlerDefinition;
import com.serotonin.m2m2.module.definitions.event.handlers.ScriptEventHandlerDefinition;
import com.serotonin.m2m2.module.definitions.event.handlers.SetPointEventHandlerDefinition;
import com.serotonin.m2m2.module.definitions.filestore.CoreFileStoreDefinition;
import com.serotonin.m2m2.module.definitions.filestore.DocsFileStoreDefinition;
import com.serotonin.m2m2.module.definitions.filestore.PublicFileStoreDefinition;
import com.serotonin.m2m2.module.definitions.permissions.ChangeOwnUsernamePermissionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.ConfigurationBackupActionPermissionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.CoreFileStoreReadPermissionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.CoreFileStoreWritePermissionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.DataSourcePermissionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.DocsFileStoreReadPermissionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.DocsFileStoreWritePermissionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.EventHandlerCreatePermission;
import com.serotonin.m2m2.module.definitions.permissions.EventsViewPermissionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.JsonDataCreatePermissionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.MailingListCreatePermission;
import com.serotonin.m2m2.module.definitions.permissions.PublicFileStoreWritePermissionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.PurgeAllEventsActionPermissionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.PurgeAllPointValuesActionPermissionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.PurgeWithPurgeSettingsActionPermissionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.SendToMailingListPermission;
import com.serotonin.m2m2.module.definitions.permissions.SqlBackupActionPermissionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.SqlRestoreActionPermissionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.SuperadminPermissionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.SystemMetricsReadPermissionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.UserCreatePermission;
import com.serotonin.m2m2.module.definitions.permissions.UserEditSelfPermission;
import com.serotonin.m2m2.module.definitions.permissions.UserFileStoreCreatePermissionDefinition;
import com.serotonin.m2m2.module.definitions.script.DataPointQueryScriptUtilityDefinition;
import com.serotonin.m2m2.module.definitions.script.DataSourceQueryScriptUtilityDefinition;
import com.serotonin.m2m2.module.definitions.script.HttpBuilderScriptUtilityDefinition;
import com.serotonin.m2m2.module.definitions.script.JsonEmportScriptUtilityDefinition;
import com.serotonin.m2m2.module.definitions.script.PointValueTimeStreamScriptUtilityDefinition;
import com.serotonin.m2m2.module.definitions.script.RuntimeManagerScriptUtilityDefinition;
import com.serotonin.m2m2.module.definitions.settings.BackupSettingsListenerDefinition;
import com.serotonin.m2m2.module.definitions.settings.DataPointTagsDisplaySettingDefinition;
import com.serotonin.m2m2.module.definitions.settings.DatabaseBackupSettingsListenerDefinition;
import com.serotonin.m2m2.module.definitions.settings.DatabaseTypeInfoDefinition;
import com.serotonin.m2m2.module.definitions.settings.DiskInfoDefinition;
import com.serotonin.m2m2.module.definitions.settings.EventsCountInfoDefinition;
import com.serotonin.m2m2.module.definitions.settings.FiledataCountInfoDefinition;
import com.serotonin.m2m2.module.definitions.settings.FiledataSizeInfoDefinition;
import com.serotonin.m2m2.module.definitions.settings.LanguageSettingListenerDefinition;
import com.serotonin.m2m2.module.definitions.settings.LastUpgradeSettingsListenerDefinition;
import com.serotonin.m2m2.module.definitions.settings.LoadAverageInfoDefinition;
import com.serotonin.m2m2.module.definitions.settings.NoSqlPointValueDatabaseSizeInfoDefinition;
import com.serotonin.m2m2.module.definitions.settings.OperatingSystemInfoDefinition;
import com.serotonin.m2m2.module.definitions.settings.SqlDatabaseBackupFileListInfoDefinition;
import com.serotonin.m2m2.module.definitions.settings.SqlDatabaseSizeInfoDefinition;
import com.serotonin.m2m2.module.definitions.settings.TimezoneInfoDefinition;
import com.serotonin.m2m2.module.license.LicenseEnforcement;
import com.serotonin.m2m2.rt.event.type.AuditEventTypeSettingsListenerDefinition;
import com.serotonin.m2m2.rt.event.type.SystemEventTypeSettingsListenerDefinition;
import com.serotonin.m2m2.rt.event.type.definition.BackupFailureEventTypeDefinition;
import com.serotonin.m2m2.rt.event.type.definition.EmailSendFailureEventTypeDefinition;
import com.serotonin.m2m2.rt.event.type.definition.FailedUserLoginEventTypeDefinition;
import com.serotonin.m2m2.rt.event.type.definition.LicenseCheckEventTypeDefinition;
import com.serotonin.m2m2.rt.event.type.definition.MaxAlarmLevelChangedEventTypeDefinition;
import com.serotonin.m2m2.rt.event.type.definition.MissingModuleDependencyEventTypeDefinition;
import com.serotonin.m2m2.rt.event.type.definition.NewUserRegisteredEventTypeDefinition;
import com.serotonin.m2m2.rt.event.type.definition.ProcessFailureEventTypeDefinition;
import com.serotonin.m2m2.rt.event.type.definition.RejectedWorkItemEventTypeDefinition;
import com.serotonin.m2m2.rt.event.type.definition.SetPointHandlerFailureEventTypeDefinition;
import com.serotonin.m2m2.rt.event.type.definition.SystemShutdownEventTypeDefinition;
import com.serotonin.m2m2.rt.event.type.definition.SystemStartupEventTypeDefinition;
import com.serotonin.m2m2.rt.event.type.definition.UpgradeCheckEventTypeDefinition;
import com.serotonin.m2m2.rt.event.type.definition.UserLoginEventTypeDefinition;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.dataSource.mock.MockDataSourceDefinition;
import com.serotonin.m2m2.vo.event.detector.AbstractEventDetectorVO;
import com.serotonin.m2m2.vo.publish.mock.MockPublisherDefinition;
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
                    for(EventHandlerDefinition<?> def : Module.getDefinitions(preDefaults, EventHandlerDefinition.class)){
                        map.put(def.getEventHandlerTypeName(), def);
                    }
                    for (Module module : MODULES.values()) {
                        for (EventHandlerDefinition<?> def : module.getDefinitions(EventHandlerDefinition.class))
                            map.put(def.getEventHandlerTypeName(), def);
                    }
                    for(EventHandlerDefinition<?> def : Module.getDefinitions(postDefaults, EventHandlerDefinition.class)){
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
                    for(EventDetectorDefinition<?> def : Module.getDefinitions(preDefaults, EventDetectorDefinition.class)){
                        map.put(def.getEventDetectorTypeName(), def);
                    }
                    for (Module module : MODULES.values()) {
                        for (EventDetectorDefinition<?> def : module.getDefinitions(EventDetectorDefinition.class))
                            map.put(def.getEventDetectorTypeName(), def);
                    }
                    for(EventDetectorDefinition<?> def : Module.getDefinitions(postDefaults, EventDetectorDefinition.class)){
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
                    for(PermissionDefinition def : Module.getDefinitions(preDefaults, PermissionDefinition.class)){
                        map.put(def.getPermissionTypeName(), def);
                    }
                    for (Module module : MODULES.values()) {
                        for (PermissionDefinition def : module.getDefinitions(PermissionDefinition.class))
                            map.put(def.getPermissionTypeName(), def);
                    }
                    for(PermissionDefinition def : Module.getDefinitions(postDefaults, PermissionDefinition.class)){
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
                    List<AngularJSModuleDefinition> list = new ArrayList<AngularJSModuleDefinition>();
                    for(AngularJSModuleDefinition def : Module.getDefinitions(preDefaults, AngularJSModuleDefinition.class)){
                        list.add(def);
                    }
                    for (Module module : MODULES.values()) {
                        for (AngularJSModuleDefinition def : module.getDefinitions(AngularJSModuleDefinition.class))
                            list.add(def);
                    }
                    for(AngularJSModuleDefinition def : Module.getDefinitions(postDefaults, AngularJSModuleDefinition.class)){
                        list.add(def);
                    }
                    list.sort((a, b) -> ((Integer) b.priority()).compareTo(a.priority()));
                    ANGULARJS_MODULE_DEFINITIONS = Collections.unmodifiableList(list);
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
                    List<SystemSettingsDefinition> list = new ArrayList<SystemSettingsDefinition>();
                    for(SystemSettingsDefinition def : Module.getDefinitions(preDefaults, SystemSettingsDefinition.class)){
                        list.add(def);
                    }
                    for (Module module : MODULES.values()) {
                        for (SystemSettingsDefinition def : module.getDefinitions(SystemSettingsDefinition.class))
                            list.add(def);
                    }
                    for(SystemSettingsDefinition def : Module.getDefinitions(postDefaults, SystemSettingsDefinition.class)){
                        list.add(def);
                    }
                    SYSTEM_SETTINGS_DEFINITIONS = list;
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
                    List<SystemSettingsListenerDefinition> list = new ArrayList<SystemSettingsListenerDefinition>();
                    for(SystemSettingsListenerDefinition def : Module.getDefinitions(preDefaults, SystemSettingsListenerDefinition.class)){
                        list.add(def);
                    }
                    for (Module module : MODULES.values()) {
                        for (SystemSettingsListenerDefinition def : module.getDefinitions(SystemSettingsListenerDefinition.class))
                            list.add(def);
                    }
                    for(SystemSettingsListenerDefinition def : Module.getDefinitions(postDefaults, SystemSettingsListenerDefinition.class)){
                        list.add(def);
                    }
                    SYSTEM_SETTINGS_LISTENER_DEFINITIONS = list;
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
                    for(SystemInfoDefinition<?> def : Module.getDefinitions(preDefaults, SystemInfoDefinition.class)){
                        map.put(def.getKey(), def);
                    }
                    for (Module module : MODULES.values()) {
                        for (SystemInfoDefinition<?> def : module.getDefinitions(SystemInfoDefinition.class))
                            map.put(def.getKey(), def);
                    }
                    for(SystemInfoDefinition<?> def : Module.getDefinitions(postDefaults, SystemInfoDefinition.class)){
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
                    for(SystemActionDefinition def : Module.getDefinitions(preDefaults, SystemActionDefinition.class)){
                        map.put(def.getKey(), def);
                    }
                    for (Module module : MODULES.values()) {
                        for (SystemActionDefinition def : module.getDefinitions(SystemActionDefinition.class))
                            map.put(def.getKey(), def);
                    }
                    for(SystemActionDefinition def : Module.getDefinitions(postDefaults, SystemActionDefinition.class)){
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
                    for(FileStoreDefinition def : Module.getDefinitions(preDefaults, FileStoreDefinition.class)){
                        map.put(def.getStoreName(), def);
                    }
                    for (Module module : MODULES.values()) {
                        for (FileStoreDefinition def : module.getDefinitions(FileStoreDefinition.class)) {
                            try {
                                def.ensureExists();
                            } catch (IOException e) {
                                LOG.error("Couldn't create directory for file store", e);
                            }
                            map.put(def.getStoreName(), def);
                        }
                    }
                    for(FileStoreDefinition def : Module.getDefinitions(postDefaults, FileStoreDefinition.class)){
                        map.put(def.getStoreName(), def);
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
                    List<MangoJavascriptContextObjectDefinition> list = new ArrayList<MangoJavascriptContextObjectDefinition>();
                    for(MangoJavascriptContextObjectDefinition def : Module.getDefinitions(preDefaults, MangoJavascriptContextObjectDefinition.class)){
                        list.add(def);
                    }
                    for (Module module : MODULES.values()) {
                        for (MangoJavascriptContextObjectDefinition def : module.getDefinitions(MangoJavascriptContextObjectDefinition.class))
                            list.add(def);
                    }
                    for(MangoJavascriptContextObjectDefinition def : Module.getDefinitions(postDefaults, MangoJavascriptContextObjectDefinition.class)){
                        list.add(def);
                    }
                    JAVASCRIPT_CONTEXT_DEFINITIONS = list;
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

            @Override
            public String getErrorPageUri(HttpServletRequest request, HttpServletResponse response) {
                return "/error.htm";
            }

            @Override
            public String getNotFoundPageUri(HttpServletRequest request, HttpServletResponse response) {
                return "/not-found.htm";
            }
        });

        /*
         * Add a module for the core
         */
        addModule(getCoreModule());

    }

    /**
     * Helper Method to create a Module with Core Information
     * @return
     */
    private static CoreModule getCoreModule(){
        CoreModule core = new CoreModule(ModuleRegistry.CORE_MODULE_NAME, Common.getVersion(), new TranslatableMessage("modules.core.description"),
                "Infinite Automation Systems, Inc.", "https://www.infiniteautomation.com", null, -1, Common.isCoreSigned());

        core.addDefinition((LicenseDefinition) Providers.get(ICoreLicense.class));

        //Add in Core Event Handlers
        core.addDefinition(new ScriptEventHandlerDefinition());
        core.addDefinition(new EmailEventHandlerDefinition());
        core.addDefinition(new ProcessEventHandlerDefinition());
        core.addDefinition(new SetPointEventHandlerDefinition());

        //Add in Core Event Detectors
        core.addDefinition(new AlphanumericRegexStateEventDetectorDefinition());
        core.addDefinition(new AlphanumericStateEventDetectorDefinition());
        core.addDefinition(new AnalogChangeEventDetectorDefinition());
        core.addDefinition(new AnalogHighLimitEventDetectorDefinition());
        core.addDefinition(new AnalogLowLimitEventDetectorDefinition());
        core.addDefinition(new AnalogRangeEventDetectorDefinition());
        core.addDefinition(new BinaryStateEventDetectorDefinition());
        core.addDefinition(new MultistateStateEventDetectorDefinition());
        core.addDefinition(new NegativeCusumEventDetectorDefinition());
        core.addDefinition(new NoChangeEventDetectorDefinition());
        core.addDefinition(new PointChangeEventDetectorDefinition());
        core.addDefinition(new PositiveCusumEventDetectorDefinition());
        core.addDefinition(new SmoothnessEventDetectorDefinition());
        core.addDefinition(new StateChangeCountEventDetectorDefinition());
        core.addDefinition(new NoUpdateEventDetectorDefinition());
        core.addDefinition(new RateOfChangeDetectorDefinition());

        /* Permissions Settings */
        core.addDefinition(new SuperadminPermissionDefinition());
        core.addDefinition(new DataSourcePermissionDefinition());
        core.addDefinition(new ConfigurationBackupActionPermissionDefinition());
        core.addDefinition(new PurgeAllEventsActionPermissionDefinition());
        core.addDefinition(new PurgeAllPointValuesActionPermissionDefinition());
        core.addDefinition(new PurgeWithPurgeSettingsActionPermissionDefinition());
        core.addDefinition(new SqlBackupActionPermissionDefinition());
        core.addDefinition(new SqlRestoreActionPermissionDefinition());
        core.addDefinition(new CoreFileStoreReadPermissionDefinition());
        core.addDefinition(new CoreFileStoreWritePermissionDefinition());
        core.addDefinition(new PublicFileStoreWritePermissionDefinition());
        core.addDefinition(new DocsFileStoreReadPermissionDefinition());
        core.addDefinition(new DocsFileStoreWritePermissionDefinition());
        core.addDefinition(new UserFileStoreCreatePermissionDefinition());
        core.addDefinition(new JsonDataCreatePermissionDefinition());
        core.addDefinition(new MailingListCreatePermission());
        core.addDefinition(new UserEditSelfPermission());
        core.addDefinition(new ChangeOwnUsernamePermissionDefinition());
        core.addDefinition(new SendToMailingListPermission());
        core.addDefinition(new UserCreatePermission());
        core.addDefinition(new EventHandlerCreatePermission());
        core.addDefinition(new SystemMetricsReadPermissionDefinition());
        core.addDefinition(new EventsViewPermissionDefinition());

        /* Read Only Settings */
        core.addDefinition(new TimezoneInfoDefinition());
        core.addDefinition(new DatabaseTypeInfoDefinition());
        core.addDefinition(new SqlDatabaseSizeInfoDefinition());
        core.addDefinition(new FiledataSizeInfoDefinition());
        core.addDefinition(new FiledataCountInfoDefinition());
        core.addDefinition(new NoSqlPointValueDatabaseSizeInfoDefinition());
        core.addDefinition(new SqlDatabaseBackupFileListInfoDefinition());
        core.addDefinition(new EventsCountInfoDefinition());
        core.addDefinition(new DiskInfoDefinition());
        core.addDefinition(new LoadAverageInfoDefinition());
        core.addDefinition(new OperatingSystemInfoDefinition());
        core.addDefinition(new DiskInfoDefinition());

        /* System Settings Listeners */
        // Do NOT Use the ThreadPoolListener as if the pools are full we can't spawn threads to update the settings...
        // core.addDefinition(new ThreadPoolSettingsListenerDefinition());
        //
        core.addDefinition(new LanguageSettingListenerDefinition());
        core.addDefinition(new BackupSettingsListenerDefinition());
        core.addDefinition(new DatabaseBackupSettingsListenerDefinition());
        core.addDefinition(new AuditEventTypeSettingsListenerDefinition());
        core.addDefinition(new SystemEventTypeSettingsListenerDefinition());
        core.addDefinition(new LastUpgradeSettingsListenerDefinition());

        /* System Actions */
        core.addDefinition(new PurgeAllPointValuesActionDefinition());
        core.addDefinition(new PurgeAllEventsActionDefinition());
        core.addDefinition(new PurgeWithPurgeSettingsActionDefinition());
        core.addDefinition(new ConfigurationBackupActionDefinition());
        core.addDefinition(new SqlBackupActionDefinition());
        core.addDefinition(new SqlRestoreActionDefinition());

        /* File Store */
        core.addDefinition(new CoreFileStoreDefinition());
        core.addDefinition(new PublicFileStoreDefinition());
        core.addDefinition(new DocsFileStoreDefinition());

        /* Script Utilities */
        core.addDefinition(new DataPointQueryScriptUtilityDefinition());
        core.addDefinition(new DataSourceQueryScriptUtilityDefinition());
        core.addDefinition(new HttpBuilderScriptUtilityDefinition());
        core.addDefinition(new JsonEmportScriptUtilityDefinition());
        core.addDefinition(new PointValueTimeStreamScriptUtilityDefinition());
        core.addDefinition(new RuntimeManagerScriptUtilityDefinition());

        /* System Event Types */
        core.addDefinition(new BackupFailureEventTypeDefinition());
        core.addDefinition(new EmailSendFailureEventTypeDefinition());
        core.addDefinition(new FailedUserLoginEventTypeDefinition());
        core.addDefinition(new LicenseCheckEventTypeDefinition());
        core.addDefinition(new MaxAlarmLevelChangedEventTypeDefinition());
        core.addDefinition(new MissingModuleDependencyEventTypeDefinition());
        core.addDefinition(new ProcessFailureEventTypeDefinition());
        core.addDefinition(new RejectedWorkItemEventTypeDefinition());
        core.addDefinition(new SetPointHandlerFailureEventTypeDefinition());
        core.addDefinition(new SystemShutdownEventTypeDefinition());
        core.addDefinition(new SystemStartupEventTypeDefinition());
        core.addDefinition(new UpgradeCheckEventTypeDefinition());
        core.addDefinition(new UserLoginEventTypeDefinition());
        core.addDefinition(new NewUserRegisteredEventTypeDefinition());

        /* System Settings Definitions */
        core.addDefinition(new DataPointTagsDisplaySettingDefinition());

        /* Test Definitions */
        if(Common.envProps.getBoolean("testing.enabled")) {
            core.addDefinition(new MockDataSourceDefinition());
            core.addDefinition(new MockPublisherDefinition());
        }

        return core;
    }

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
