/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.dwr;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.infiniteautomation.mango.io.serial.virtual.SerialServerSocketBridgeConfig;
import com.infiniteautomation.mango.io.serial.virtual.SerialSocketBridgeConfig;
import com.infiniteautomation.mango.io.serial.virtual.VirtualSerialPortConfig;
import com.infiniteautomation.mango.io.serial.virtual.VirtualSerialPortConfigDao;
import com.serotonin.InvalidArgumentException;
import com.serotonin.db.pair.StringStringPair;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.EventDao;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.email.MangoEmailContent;
import com.serotonin.m2m2.i18n.ProcessMessage;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.SuperadminPermissionDefinition;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.rt.event.type.SystemEventType;
import com.serotonin.m2m2.rt.maint.BackgroundProcessing;
import com.serotonin.m2m2.rt.maint.DataPurge;
import com.serotonin.m2m2.rt.maint.work.BackupWorkItem;
import com.serotonin.m2m2.rt.maint.work.DatabaseBackupWorkItem;
import com.serotonin.m2m2.rt.maint.work.EmailWorkItem;
import com.serotonin.m2m2.util.ColorUtils;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.bean.PointHistoryCount;
import com.serotonin.m2m2.vo.permission.Permissions;
import com.serotonin.m2m2.web.dwr.beans.EventTypeVOHandlers;
import com.serotonin.m2m2.web.dwr.util.DwrPermission;
import com.serotonin.util.DirectoryInfo;
import com.serotonin.util.DirectoryUtils;

public class SystemSettingsDwr extends BaseDwr {

    private static final String EMAIL_PATTERN =
            "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
                    + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
    private Pattern emailPattern = Pattern.compile(EMAIL_PATTERN);


    @DwrPermission(admin = true)
    public Map<String, Object> getSettings() {
        Map<String, Object> settings = new HashMap<>();

        // Info
        settings.put(SystemSettingsDao.INSTANCE_DESCRIPTION,
                SystemSettingsDao.instance.getValue(SystemSettingsDao.INSTANCE_DESCRIPTION));

        // Email
        settings.put(SystemSettingsDao.EMAIL_SMTP_HOST, SystemSettingsDao.instance.getValue(SystemSettingsDao.EMAIL_SMTP_HOST));
        settings.put(SystemSettingsDao.EMAIL_SMTP_PORT,
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.EMAIL_SMTP_PORT));
        settings.put(SystemSettingsDao.EMAIL_FROM_ADDRESS,
                SystemSettingsDao.instance.getValue(SystemSettingsDao.EMAIL_FROM_ADDRESS));
        settings.put(SystemSettingsDao.EMAIL_FROM_NAME, SystemSettingsDao.instance.getValue(SystemSettingsDao.EMAIL_FROM_NAME));
        settings.put(SystemSettingsDao.EMAIL_AUTHORIZATION,
                SystemSettingsDao.instance.getBooleanValue(SystemSettingsDao.EMAIL_AUTHORIZATION));
        settings.put(SystemSettingsDao.EMAIL_SMTP_USERNAME,
                SystemSettingsDao.instance.getValue(SystemSettingsDao.EMAIL_SMTP_USERNAME));
        settings.put(SystemSettingsDao.EMAIL_SMTP_PASSWORD,
                SystemSettingsDao.instance.getValue(SystemSettingsDao.EMAIL_SMTP_PASSWORD));
        settings.put(SystemSettingsDao.EMAIL_TLS, SystemSettingsDao.instance.getBooleanValue(SystemSettingsDao.EMAIL_TLS));
        settings.put(SystemSettingsDao.EMAIL_CONTENT_TYPE,
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.EMAIL_CONTENT_TYPE));

        // System event types
        settings.put("systemEventTypes", SystemEventType.getRegisteredEventTypes()
                .stream()
                .map(x -> new EventTypeVOHandlers(x))
                .collect(Collectors.toList()));

        // System event types
        settings.put("auditEventTypes", AuditEventType.getRegisteredEventTypes()
                .stream()
                .map(x -> new EventTypeVOHandlers(x))
                .collect(Collectors.toList()));

        // Http Client
        settings.put(SystemSettingsDao.HTTP_CLIENT_USE_PROXY,
                SystemSettingsDao.instance.getBooleanValue(SystemSettingsDao.HTTP_CLIENT_USE_PROXY));
        settings.put(SystemSettingsDao.HTTP_CLIENT_PROXY_SERVER,
                SystemSettingsDao.instance.getValue(SystemSettingsDao.HTTP_CLIENT_PROXY_SERVER));
        settings.put(SystemSettingsDao.HTTP_CLIENT_PROXY_PORT,
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.HTTP_CLIENT_PROXY_PORT));
        settings.put(SystemSettingsDao.HTTP_CLIENT_PROXY_USERNAME,
                SystemSettingsDao.instance.getValue(SystemSettingsDao.HTTP_CLIENT_PROXY_USERNAME));
        settings.put(SystemSettingsDao.HTTP_CLIENT_PROXY_PASSWORD,
                SystemSettingsDao.instance.getValue(SystemSettingsDao.HTTP_CLIENT_PROXY_PASSWORD));

        // Misc
        settings.put(SystemSettingsDao.POINT_DATA_PURGE_PERIOD_TYPE,
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.POINT_DATA_PURGE_PERIOD_TYPE));
        settings.put(SystemSettingsDao.POINT_DATA_PURGE_PERIODS,
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.POINT_DATA_PURGE_PERIODS));

        settings.put(DataPurge.ENABLE_POINT_DATA_PURGE, SystemSettingsDao.instance.getBooleanValue(DataPurge.ENABLE_POINT_DATA_PURGE, true));
        settings.put(SystemSettingsDao.POINT_DATA_PURGE_COUNT, SystemSettingsDao.instance.getBooleanValue(SystemSettingsDao.POINT_DATA_PURGE_COUNT));

        settings.put(SystemSettingsDao.DATA_POINT_EVENT_PURGE_PERIOD_TYPE,
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.DATA_POINT_EVENT_PURGE_PERIOD_TYPE));
        settings.put(SystemSettingsDao.DATA_POINT_EVENT_PURGE_PERIODS,
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.DATA_POINT_EVENT_PURGE_PERIODS));
        settings.put(SystemSettingsDao.DATA_SOURCE_EVENT_PURGE_PERIOD_TYPE,
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.DATA_SOURCE_EVENT_PURGE_PERIOD_TYPE));
        settings.put(SystemSettingsDao.DATA_SOURCE_EVENT_PURGE_PERIODS,
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.DATA_SOURCE_EVENT_PURGE_PERIODS));
        settings.put(SystemSettingsDao.SYSTEM_EVENT_PURGE_PERIOD_TYPE,
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.SYSTEM_EVENT_PURGE_PERIOD_TYPE));
        settings.put(SystemSettingsDao.SYSTEM_EVENT_PURGE_PERIODS,
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.SYSTEM_EVENT_PURGE_PERIODS));
        settings.put(SystemSettingsDao.PUBLISHER_EVENT_PURGE_PERIOD_TYPE,
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.PUBLISHER_EVENT_PURGE_PERIOD_TYPE));
        settings.put(SystemSettingsDao.PUBLISHER_EVENT_PURGE_PERIODS,
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.PUBLISHER_EVENT_PURGE_PERIODS));
        settings.put(SystemSettingsDao.AUDIT_EVENT_PURGE_PERIOD_TYPE,
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.AUDIT_EVENT_PURGE_PERIOD_TYPE));
        settings.put(SystemSettingsDao.AUDIT_EVENT_PURGE_PERIODS,
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.AUDIT_EVENT_PURGE_PERIODS));

        settings.put(SystemSettingsDao.NONE_ALARM_PURGE_PERIOD_TYPE,
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.NONE_ALARM_PURGE_PERIOD_TYPE));
        settings.put(SystemSettingsDao.NONE_ALARM_PURGE_PERIODS,
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.NONE_ALARM_PURGE_PERIODS));
        settings.put(SystemSettingsDao.INFORMATION_ALARM_PURGE_PERIOD_TYPE,
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.INFORMATION_ALARM_PURGE_PERIOD_TYPE));
        settings.put(SystemSettingsDao.INFORMATION_ALARM_PURGE_PERIODS,
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.INFORMATION_ALARM_PURGE_PERIODS));
        settings.put(SystemSettingsDao.IMPORTANT_ALARM_PURGE_PERIOD_TYPE,
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.IMPORTANT_ALARM_PURGE_PERIOD_TYPE));
        settings.put(SystemSettingsDao.IMPORTANT_ALARM_PURGE_PERIODS,
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.IMPORTANT_ALARM_PURGE_PERIODS));
        settings.put(SystemSettingsDao.WARNING_ALARM_PURGE_PERIOD_TYPE,
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.WARNING_ALARM_PURGE_PERIOD_TYPE));
        settings.put(SystemSettingsDao.WARNING_ALARM_PURGE_PERIODS,
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.WARNING_ALARM_PURGE_PERIODS));
        settings.put(SystemSettingsDao.URGENT_ALARM_PURGE_PERIOD_TYPE,
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.URGENT_ALARM_PURGE_PERIOD_TYPE));
        settings.put(SystemSettingsDao.URGENT_ALARM_PURGE_PERIODS,
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.URGENT_ALARM_PURGE_PERIODS));
        settings.put(SystemSettingsDao.CRITICAL_ALARM_PURGE_PERIOD_TYPE,
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.CRITICAL_ALARM_PURGE_PERIOD_TYPE));
        settings.put(SystemSettingsDao.CRITICAL_ALARM_PURGE_PERIODS,
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.CRITICAL_ALARM_PURGE_PERIODS));
        settings.put(SystemSettingsDao.LIFE_SAFETY_ALARM_PURGE_PERIOD_TYPE,
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.LIFE_SAFETY_ALARM_PURGE_PERIOD_TYPE));
        settings.put(SystemSettingsDao.LIFE_SAFETY_ALARM_PURGE_PERIODS,
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.LIFE_SAFETY_ALARM_PURGE_PERIODS));

        settings.put(SystemSettingsDao.EVENT_PURGE_PERIOD_TYPE,
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.EVENT_PURGE_PERIOD_TYPE));
        settings.put(SystemSettingsDao.EVENT_PURGE_PERIODS,
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.EVENT_PURGE_PERIODS));

        settings.put(SystemSettingsDao.UI_PERFORMANCE, SystemSettingsDao.instance.getIntValue(SystemSettingsDao.UI_PERFORMANCE));
        settings.put(SystemSettingsDao.FUTURE_DATE_LIMIT_PERIOD_TYPE,
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.FUTURE_DATE_LIMIT_PERIOD_TYPE));
        settings.put(SystemSettingsDao.FUTURE_DATE_LIMIT_PERIODS,
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.FUTURE_DATE_LIMIT_PERIODS));

        // Language
        settings.put(SystemSettingsDao.LANGUAGE, SystemSettingsDao.instance.getValue(SystemSettingsDao.LANGUAGE));

        // Colours
        settings.put(SystemSettingsDao.CHART_BACKGROUND_COLOUR,
                SystemSettingsDao.instance.getValue(SystemSettingsDao.CHART_BACKGROUND_COLOUR));
        settings.put(SystemSettingsDao.PLOT_BACKGROUND_COLOUR,
                SystemSettingsDao.instance.getValue(SystemSettingsDao.PLOT_BACKGROUND_COLOUR));
        settings.put(SystemSettingsDao.PLOT_GRIDLINE_COLOUR,
                SystemSettingsDao.instance.getValue(SystemSettingsDao.PLOT_GRIDLINE_COLOUR));

        //Backup Settings
        settings.put(SystemSettingsDao.BACKUP_FILE_LOCATION,
                SystemSettingsDao.instance.getValue(SystemSettingsDao.BACKUP_FILE_LOCATION));
        settings.put(SystemSettingsDao.BACKUP_PERIOD_TYPE,
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.BACKUP_PERIOD_TYPE));
        settings.put(SystemSettingsDao.BACKUP_PERIODS, SystemSettingsDao.instance.getIntValue(SystemSettingsDao.BACKUP_PERIODS));
        try {

            SimpleDateFormat sdf = new SimpleDateFormat("MMM-dd-yyyy HH:mm:ss");
            String lastRunString = SystemSettingsDao.instance.getValue(SystemSettingsDao.BACKUP_LAST_RUN_SUCCESS);
            if(lastRunString != null){
                Date lastRunDate = BackupWorkItem.dateFormatter.parse(lastRunString);
                lastRunString = sdf.format(lastRunDate);
                settings.put(SystemSettingsDao.BACKUP_LAST_RUN_SUCCESS, lastRunString);
            }else{
                settings.put(SystemSettingsDao.BACKUP_LAST_RUN_SUCCESS, "unknown");
            }
        }
        catch (Exception e) {
            settings.put(SystemSettingsDao.BACKUP_LAST_RUN_SUCCESS, "unknown");
        }
        settings.put(SystemSettingsDao.BACKUP_HOUR, SystemSettingsDao.instance.getIntValue(SystemSettingsDao.BACKUP_HOUR));
        settings.put(SystemSettingsDao.BACKUP_MINUTE, SystemSettingsDao.instance.getIntValue(SystemSettingsDao.BACKUP_MINUTE));
        settings.put(SystemSettingsDao.BACKUP_FILE_COUNT,
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.BACKUP_FILE_COUNT));
        //Have to have a default value due to the lack of use of DEFAULT_VALUES for bools
        settings.put(SystemSettingsDao.BACKUP_ENABLED,
                SystemSettingsDao.instance.getBooleanValue(SystemSettingsDao.BACKUP_ENABLED));

        //Have to have a default value due to the lack of use of DEFAULT_VALUES for bools
        settings.put(SystemSettingsDao.ALLOW_ANONYMOUS_CHART_VIEW,
                SystemSettingsDao.instance.getBooleanValue(SystemSettingsDao.ALLOW_ANONYMOUS_CHART_VIEW, false));
        settings.put(SystemSettingsDao.JFREE_CHART_FONT,
                SystemSettingsDao.instance.getValue(SystemSettingsDao.JFREE_CHART_FONT, ""));

        //Database Backup Settings
        settings.put(SystemSettingsDao.DATABASE_BACKUP_FILE_LOCATION,
                SystemSettingsDao.instance.getValue(SystemSettingsDao.DATABASE_BACKUP_FILE_LOCATION));
        settings.put(SystemSettingsDao.DATABASE_BACKUP_PERIOD_TYPE,
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.DATABASE_BACKUP_PERIOD_TYPE));
        settings.put(SystemSettingsDao.DATABASE_BACKUP_PERIODS,
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.DATABASE_BACKUP_PERIODS));
        try {

            SimpleDateFormat sdf = new SimpleDateFormat("MMM-dd-yyyy HH:mm:ss");
            String lastRunString = SystemSettingsDao.instance.getValue(SystemSettingsDao.DATABASE_BACKUP_LAST_RUN_SUCCESS);
            if(lastRunString != null){
                Date lastRunDate = BackupWorkItem.dateFormatter.parse(lastRunString);
                lastRunString = sdf.format(lastRunDate);
                settings.put(SystemSettingsDao.DATABASE_BACKUP_LAST_RUN_SUCCESS, lastRunString);
            }else{
                settings.put(SystemSettingsDao.DATABASE_BACKUP_LAST_RUN_SUCCESS, "unknown");
            }
        }
        catch (Exception e) {
            settings.put(SystemSettingsDao.DATABASE_BACKUP_LAST_RUN_SUCCESS, "unknown");
        }
        settings.put(SystemSettingsDao.DATABASE_BACKUP_HOUR,
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.DATABASE_BACKUP_HOUR));
        settings.put(SystemSettingsDao.DATABASE_BACKUP_MINUTE,
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.DATABASE_BACKUP_MINUTE));
        settings.put(SystemSettingsDao.DATABASE_BACKUP_FILE_COUNT,
                SystemSettingsDao.instance.getIntValue(SystemSettingsDao.DATABASE_BACKUP_FILE_COUNT));
        //Have to have a default value due to the lack of use of DEFAULT_VALUES for bools
        settings.put(SystemSettingsDao.DATABASE_BACKUP_ENABLED,
                SystemSettingsDao.instance.getBooleanValue(SystemSettingsDao.DATABASE_BACKUP_ENABLED));

        // Permissions
        List<Map<String, String>> modulePermissions = new ArrayList<>();
        settings.put("modulePermissions", modulePermissions);
        for (PermissionDefinition def : ModuleRegistry.getDefinitions(PermissionDefinition.class)) {
            if(!def.getPermissionTypeName().equals(SuperadminPermissionDefinition.PERMISSION)) {
                Map<String, String> permission = new HashMap<>();
                permission.put("name", def.getPermissionTypeName());
                permission.put("label", Translations.getTranslations(Common.getLocale()).translate(def.getPermissionKey()));
                permission.put("value", SystemSettingsDao.instance.getValue(def.getPermissionTypeName()));
                modulePermissions.add(permission);
            }
        }

        //Thread Pool Settings
        settings.put(SystemSettingsDao.HIGH_PRI_CORE_POOL_SIZE, SystemSettingsDao.instance.getIntValue(SystemSettingsDao.HIGH_PRI_CORE_POOL_SIZE));
        settings.put(SystemSettingsDao.HIGH_PRI_MAX_POOL_SIZE, SystemSettingsDao.instance.getIntValue(SystemSettingsDao.HIGH_PRI_MAX_POOL_SIZE));
        settings.put(SystemSettingsDao.MED_PRI_CORE_POOL_SIZE, SystemSettingsDao.instance.getIntValue(SystemSettingsDao.MED_PRI_CORE_POOL_SIZE));
        settings.put(SystemSettingsDao.LOW_PRI_CORE_POOL_SIZE, SystemSettingsDao.instance.getIntValue(SystemSettingsDao.LOW_PRI_CORE_POOL_SIZE));

        //Virtual Serial Ports
        settings.put("virtualSerialPorts", VirtualSerialPortConfigDao.getInstance().getAll());

        //Site analytics
        settings.put(SystemSettingsDao.SITE_ANALYTICS_HEAD, SystemSettingsDao.instance.getValue(SystemSettingsDao.SITE_ANALYTICS_HEAD));
        settings.put(SystemSettingsDao.SITE_ANALYTICS_BODY, SystemSettingsDao.instance.getValue(SystemSettingsDao.SITE_ANALYTICS_BODY));

        //Upgrade states
        settings.put(SystemSettingsDao.UPGRADE_VERSION_STATE, SystemSettingsDao.instance.getIntValue(SystemSettingsDao.UPGRADE_VERSION_STATE));

        //Point Hierarchy
        settings.put(SystemSettingsDao.EXPORT_HIERARCHY_PATH, SystemSettingsDao.instance.getBooleanValue(SystemSettingsDao.EXPORT_HIERARCHY_PATH));
        settings.put(SystemSettingsDao.HIERARCHY_PATH_SEPARATOR, SystemSettingsDao.instance.getValue(SystemSettingsDao.HIERARCHY_PATH_SEPARATOR));

        //Http Server
        settings.put(SystemSettingsDao.HTTP_SESSION_TIMEOUT_PERIOD_TYPE, SystemSettingsDao.instance.getIntValue(SystemSettingsDao.HTTP_SESSION_TIMEOUT_PERIOD_TYPE));
        settings.put(SystemSettingsDao.HTTP_SESSION_TIMEOUT_PERIODS, SystemSettingsDao.instance.getIntValue(SystemSettingsDao.HTTP_SESSION_TIMEOUT_PERIODS));

        return settings;
    }

    @DwrPermission(admin = true)
    public Map<String, Object> getDatabaseSize() {
        Map<String, Object> data = new HashMap<>();

        // Database size
        Long dbSize = Common.databaseProxy.getDatabaseSizeInBytes();
        if (dbSize != null) {
            data.put("databaseSize", DirectoryUtils.bytesDescription(dbSize));
        }
        else {
            data.put("databaseSize", "(" + translate("common.unknown") + ")");
            dbSize = 0L;
        }

        //Do we have any NoSQL Data
        long noSqlSize = 0L;
        if (Common.databaseProxy.getNoSQLProxy() != null) {
            String pointValueStoreName = Common.envProps.getString("db.nosql.pointValueStoreName", "mangoTSDB");
            noSqlSize = Common.databaseProxy.getNoSQLProxy().getDatabaseSizeInBytes(pointValueStoreName);
            data.put("noSqlDatabaseSize", DirectoryUtils.bytesDescription(noSqlSize));
        }

        // Filedata data
        DirectoryInfo fileDatainfo = DirectoryUtils.getSize(new File(Common.getFiledataPath()));
        long filedataSize = fileDatainfo.getSize();
        data.put("filedataCount", fileDatainfo.getCount());
        data.put("filedataSize", DirectoryUtils.bytesDescription(filedataSize));

        data.put("totalSize", DirectoryUtils.bytesDescription(dbSize + filedataSize + noSqlSize));

        // Point history counts.
        List<PointHistoryCount> counts = DataPointDao.getInstance().getTopPointHistoryCounts();
        int sum = 0;
        for (PointHistoryCount c : counts)
            sum += c.getCount();

        data.put("historyCount", sum);
        data.put("topPoints", counts);
        data.put("eventCount", EventDao.getInstance().getEventCount());

        return data;
    }

    @DwrPermission(admin = true)
    public ProcessResult saveHierarchySettings(boolean exportHierarchyPath, String hierarchyPathSeparator) {
        ProcessResult response = new ProcessResult();
        if(StringUtils.isEmpty(hierarchyPathSeparator))
            response.addContextualMessage(SystemSettingsDao.HIERARCHY_PATH_SEPARATOR, "validate.cannotContainEmptyString");
        else {
            SystemSettingsDao.instance.setBooleanValue(SystemSettingsDao.EXPORT_HIERARCHY_PATH, exportHierarchyPath);
            SystemSettingsDao.instance.setValue(SystemSettingsDao.HIERARCHY_PATH_SEPARATOR, hierarchyPathSeparator);
        }

        return response;
    }

    @DwrPermission(admin = true)
    public ProcessResult saveEmailSettings(String host, int port, String from, String name, boolean auth, String username,
            String password, boolean tls, int contentType) {

        ProcessResult response = new ProcessResult();
        SystemSettingsDao systemSettingsDao = SystemSettingsDao.instance;

        if(port < 0)
            response.addContextualMessage(SystemSettingsDao.EMAIL_SMTP_PORT, "validate.cannotBeNegative");
        if(!emailPattern.matcher(from).matches())
            response.addContextualMessage(SystemSettingsDao.EMAIL_FROM_ADDRESS, "validate.invalidValue");

        //If valid then save all
        if(!response.getHasMessages()){
            systemSettingsDao.setValue(SystemSettingsDao.EMAIL_SMTP_HOST, host);
            systemSettingsDao.setIntValue(SystemSettingsDao.EMAIL_SMTP_PORT, port);
            systemSettingsDao.setValue(SystemSettingsDao.EMAIL_FROM_ADDRESS, from);
            systemSettingsDao.setValue(SystemSettingsDao.EMAIL_FROM_NAME, name);
            systemSettingsDao.setBooleanValue(SystemSettingsDao.EMAIL_AUTHORIZATION, auth);
            systemSettingsDao.setValue(SystemSettingsDao.EMAIL_SMTP_USERNAME, username);
            systemSettingsDao.setValue(SystemSettingsDao.EMAIL_SMTP_PASSWORD, password);
            systemSettingsDao.setBooleanValue(SystemSettingsDao.EMAIL_TLS, tls);
            systemSettingsDao.setIntValue(SystemSettingsDao.EMAIL_CONTENT_TYPE, contentType);
        }

        return response;
    }

    @DwrPermission(admin = true)
    public ProcessResult saveSiteAnalytics(String siteAnalyticsHead, String siteAnalyticsBody) {

        ProcessResult response = new ProcessResult();
        SystemSettingsDao systemSettingsDao =  SystemSettingsDao.instance;

        //TODO Add some validation, not sure what yet

        //If valid then save all
        if(!response.getHasMessages()){
            systemSettingsDao.setValue(SystemSettingsDao.SITE_ANALYTICS_HEAD, siteAnalyticsHead);
            systemSettingsDao.setValue(SystemSettingsDao.SITE_ANALYTICS_BODY, siteAnalyticsBody);
        }

        return response;
    }

    @DwrPermission(admin = true)
    public ProcessResult saveThreadPoolSettings(int highPriorityCorePoolSize, int highPriorityMaxPoolSize,
            int medPriorityCorePoolSize, int lowPriorityCorePoolSize) {
        ProcessResult response = new ProcessResult();

        SystemSettingsDao systemSettingsDao = SystemSettingsDao.instance;

        if(highPriorityMaxPoolSize < BackgroundProcessing.HIGH_PRI_MAX_POOL_SIZE_MIN){
            response.addContextualMessage(SystemSettingsDao.HIGH_PRI_MAX_POOL_SIZE, "validate.greaterThanOrEqualTo", BackgroundProcessing.HIGH_PRI_MAX_POOL_SIZE_MIN);
        }else if(highPriorityMaxPoolSize < highPriorityCorePoolSize){
            response.addContextualMessage(SystemSettingsDao.HIGH_PRI_MAX_POOL_SIZE, "systemSettings.threadPools.validate.maxPoolMustBeGreaterThanCorePool");
        }else{
            systemSettingsDao.setIntValue(SystemSettingsDao.HIGH_PRI_MAX_POOL_SIZE, highPriorityMaxPoolSize);
        }

        if(highPriorityCorePoolSize > 0){
            systemSettingsDao.setIntValue(SystemSettingsDao.HIGH_PRI_CORE_POOL_SIZE, highPriorityCorePoolSize);
        }else{
            response.addContextualMessage(SystemSettingsDao.HIGH_PRI_CORE_POOL_SIZE, "validate.greaterThanZero");
        }

        //For Medium and Low the Max has no effect because they use a LinkedBlockingQueue and will just block until a
        // core pool thread is available
        if(medPriorityCorePoolSize >= BackgroundProcessing.MED_PRI_MAX_POOL_SIZE_MIN){
            //Due to the pool type we should set these to the same values
            systemSettingsDao.setIntValue(SystemSettingsDao.MED_PRI_CORE_POOL_SIZE, medPriorityCorePoolSize);
        }else{
            response.addContextualMessage(SystemSettingsDao.MED_PRI_CORE_POOL_SIZE, "validate.greaterThanOrEqualTo", BackgroundProcessing.MED_PRI_MAX_POOL_SIZE_MIN);
        }

        if(lowPriorityCorePoolSize >= BackgroundProcessing.LOW_PRI_MAX_POOL_SIZE_MIN){
            systemSettingsDao.setIntValue(SystemSettingsDao.LOW_PRI_CORE_POOL_SIZE, lowPriorityCorePoolSize);
        }else{
            response.addContextualMessage(SystemSettingsDao.LOW_PRI_CORE_POOL_SIZE, "validate.greaterThanOrEqualTo", BackgroundProcessing.LOW_PRI_MAX_POOL_SIZE_MIN);
        }
        return response;
    }

    @DwrPermission(admin = true)
    public Map<String, Object> sendTestEmail(String host, int port, String from, String name, boolean auth,
            String username, String password, boolean tls, int contentType) {
        // Save the settings
        saveEmailSettings(host, port, from, name, auth, username, password, tls, contentType);

        // Get the web context information
        User user = Common.getHttpUser();

        Map<String, Object> result = new HashMap<>();
        try {
            Translations translations = getTranslations();
            Map<String, Object> model = new HashMap<>();
            model.put("message", new TranslatableMessage("systemSettings.testEmail"));
            MangoEmailContent cnt = new MangoEmailContent("testEmail", model, translations,
                    translations.translate("ftl.testEmail"), Common.UTF8);
            EmailWorkItem.queueEmail(user.getEmail(), cnt);
            result.put("message", new TranslatableMessage("common.testEmailSent", user.getEmail()));
        }
        catch (Exception e) {
            result.put("exception", e.getMessage());
        }
        return result;
    }

    @DwrPermission(admin = true)
    public void saveSystemEventAlarmLevels(Map<String, AlarmLevels> eventAlarmLevels) {
        for (Entry<String, AlarmLevels> eventAlarmLevel : eventAlarmLevels.entrySet())
            SystemEventType.setEventTypeAlarmLevel(eventAlarmLevel.getKey(), eventAlarmLevel.getValue());
    }

    @DwrPermission(admin = true)
    public void saveAuditEventAlarmLevels(Map<String, AlarmLevels> eventAlarmLevels) {
        for (Entry<String, AlarmLevels> eventAlarmLevel : eventAlarmLevels.entrySet())
            AuditEventType.setEventTypeAlarmLevel(eventAlarmLevel.getKey(), eventAlarmLevel.getValue());
    }

    @DwrPermission(admin = true)
    public void saveHttpSettings(boolean useProxy, String host, int port, String username, String password) {
        SystemSettingsDao systemSettingsDao =  SystemSettingsDao.instance;
        systemSettingsDao.setBooleanValue(SystemSettingsDao.HTTP_CLIENT_USE_PROXY, useProxy);
        systemSettingsDao.setValue(SystemSettingsDao.HTTP_CLIENT_PROXY_SERVER, host);
        systemSettingsDao.setIntValue(SystemSettingsDao.HTTP_CLIENT_PROXY_PORT, port);
        systemSettingsDao.setValue(SystemSettingsDao.HTTP_CLIENT_PROXY_USERNAME, username);
        systemSettingsDao.setValue(SystemSettingsDao.HTTP_CLIENT_PROXY_PASSWORD, password);
    }

    @DwrPermission(admin = true)
    public void saveHttpServerSettings(int httpSessionTimeoutPeriods, int httpSessionTimeoutPeriodType) {
        SystemSettingsDao systemSettingsDao =  SystemSettingsDao.instance;
        systemSettingsDao.setIntValue(SystemSettingsDao.HTTP_SESSION_TIMEOUT_PERIODS, httpSessionTimeoutPeriods);
        systemSettingsDao.setIntValue(SystemSettingsDao.HTTP_SESSION_TIMEOUT_PERIOD_TYPE, httpSessionTimeoutPeriodType);
    }

    @DwrPermission(admin = true)
    public void saveMiscSettings(int pointDataPurgePeriodType, int pointDataPurgePeriods,
            int dataPointEventPurgePeriodType, int dataPointEventPurgePeriods, boolean pointDataPurgeEnabled,
            boolean countPurgedPointValues, int dataSourceEventPurgePeriodType,
            int dataSourceEventPurgePeriods, int systemEventPurgePeriodType, int systemEventPurgePeriods,
            int publisherEventPurgePeriodType, int publisherEventPurgePeriods, int auditEventPurgePeriodType,
            int auditEventPurgePeriods, int noneAlarmPurgePeriodType, int noneAlarmPurgePeriods,
            int informationAlarmPurgePeriodType, int informationAlarmPurgePeriods, int importantAlarmPurgePeriodType,
            int importantAlarmPurgePeriods, int warningAlarmPurgePeriodType, int warningAlarmPurgePeriods, int urgentAlarmPurgePeriodType,
            int urgentAlarmPurgePeriods, int criticalAlarmPurgePeriodType, int criticalAlarmPurgePeriods,
            int lifeSafetyAlarmPurgePeriodType, int lifeSafetyAlarmPurgePeriods, int eventPurgePeriodType,
            int eventPurgePeriods, int uiPerformance, int futureDateLimitPeriodType, int futureDateLimitPeriods) {
        SystemSettingsDao systemSettingsDao =  SystemSettingsDao.instance;
        systemSettingsDao.setIntValue(SystemSettingsDao.POINT_DATA_PURGE_PERIOD_TYPE, pointDataPurgePeriodType);
        systemSettingsDao.setIntValue(SystemSettingsDao.POINT_DATA_PURGE_PERIODS, pointDataPurgePeriods);

        systemSettingsDao.setIntValue(SystemSettingsDao.DATA_POINT_EVENT_PURGE_PERIOD_TYPE,
                dataPointEventPurgePeriodType);
        systemSettingsDao.setIntValue(SystemSettingsDao.DATA_POINT_EVENT_PURGE_PERIODS, dataPointEventPurgePeriods);

        systemSettingsDao.setBooleanValue(DataPurge.ENABLE_POINT_DATA_PURGE, pointDataPurgeEnabled);
        systemSettingsDao.setBooleanValue(SystemSettingsDao.POINT_DATA_PURGE_COUNT, countPurgedPointValues);

        systemSettingsDao.setIntValue(SystemSettingsDao.DATA_SOURCE_EVENT_PURGE_PERIOD_TYPE,
                dataSourceEventPurgePeriodType);
        systemSettingsDao.setIntValue(SystemSettingsDao.DATA_SOURCE_EVENT_PURGE_PERIODS, dataSourceEventPurgePeriods);
        systemSettingsDao.setIntValue(SystemSettingsDao.SYSTEM_EVENT_PURGE_PERIOD_TYPE, systemEventPurgePeriodType);
        systemSettingsDao.setIntValue(SystemSettingsDao.SYSTEM_EVENT_PURGE_PERIODS, systemEventPurgePeriods);
        systemSettingsDao.setIntValue(SystemSettingsDao.PUBLISHER_EVENT_PURGE_PERIOD_TYPE,
                publisherEventPurgePeriodType);
        systemSettingsDao.setIntValue(SystemSettingsDao.PUBLISHER_EVENT_PURGE_PERIODS, publisherEventPurgePeriods);
        systemSettingsDao.setIntValue(SystemSettingsDao.AUDIT_EVENT_PURGE_PERIOD_TYPE, auditEventPurgePeriodType);
        systemSettingsDao.setIntValue(SystemSettingsDao.AUDIT_EVENT_PURGE_PERIODS, auditEventPurgePeriods);

        systemSettingsDao.setIntValue(SystemSettingsDao.NONE_ALARM_PURGE_PERIOD_TYPE, noneAlarmPurgePeriodType);
        systemSettingsDao.setIntValue(SystemSettingsDao.NONE_ALARM_PURGE_PERIODS, noneAlarmPurgePeriods);
        systemSettingsDao.setIntValue(SystemSettingsDao.INFORMATION_ALARM_PURGE_PERIOD_TYPE,
                informationAlarmPurgePeriodType);
        systemSettingsDao.setIntValue(SystemSettingsDao.INFORMATION_ALARM_PURGE_PERIODS, informationAlarmPurgePeriods);
        systemSettingsDao.setIntValue(SystemSettingsDao.IMPORTANT_ALARM_PURGE_PERIOD_TYPE, importantAlarmPurgePeriodType);
        systemSettingsDao.setIntValue(SystemSettingsDao.IMPORTANT_ALARM_PURGE_PERIODS, importantAlarmPurgePeriods);
        systemSettingsDao.setIntValue(SystemSettingsDao.WARNING_ALARM_PURGE_PERIOD_TYPE, warningAlarmPurgePeriodType);
        systemSettingsDao.setIntValue(SystemSettingsDao.WARNING_ALARM_PURGE_PERIODS, warningAlarmPurgePeriods);
        systemSettingsDao.setIntValue(SystemSettingsDao.URGENT_ALARM_PURGE_PERIOD_TYPE, urgentAlarmPurgePeriodType);
        systemSettingsDao.setIntValue(SystemSettingsDao.URGENT_ALARM_PURGE_PERIODS, urgentAlarmPurgePeriods);
        systemSettingsDao.setIntValue(SystemSettingsDao.CRITICAL_ALARM_PURGE_PERIOD_TYPE, criticalAlarmPurgePeriodType);
        systemSettingsDao.setIntValue(SystemSettingsDao.CRITICAL_ALARM_PURGE_PERIODS, criticalAlarmPurgePeriods);
        systemSettingsDao.setIntValue(SystemSettingsDao.LIFE_SAFETY_ALARM_PURGE_PERIOD_TYPE,
                lifeSafetyAlarmPurgePeriodType);
        systemSettingsDao.setIntValue(SystemSettingsDao.LIFE_SAFETY_ALARM_PURGE_PERIODS, lifeSafetyAlarmPurgePeriods);

        systemSettingsDao.setIntValue(SystemSettingsDao.EVENT_PURGE_PERIOD_TYPE, eventPurgePeriodType);
        systemSettingsDao.setIntValue(SystemSettingsDao.EVENT_PURGE_PERIODS, eventPurgePeriods);
        systemSettingsDao.setIntValue(SystemSettingsDao.UI_PERFORMANCE, uiPerformance);
        systemSettingsDao.setIntValue(SystemSettingsDao.FUTURE_DATE_LIMIT_PERIOD_TYPE, futureDateLimitPeriodType);
        systemSettingsDao.setIntValue(SystemSettingsDao.FUTURE_DATE_LIMIT_PERIODS, futureDateLimitPeriods);
    }

    @DwrPermission(admin = true)
    public void saveInfoSettings(String instanceDescription, String upgradeVersionState) {
        SystemSettingsDao systemSettingsDao =  SystemSettingsDao.instance;
        systemSettingsDao.setValue(SystemSettingsDao.INSTANCE_DESCRIPTION, instanceDescription);
        systemSettingsDao.setValue(SystemSettingsDao.UPGRADE_VERSION_STATE, upgradeVersionState);
    }

    @DwrPermission(admin = true)
    public ProcessResult saveSystemPermissions(String datasource, List<StringStringPair> modulePermissions) {
        ProcessResult result = new ProcessResult();
        List<ProcessMessage> messages = new ArrayList<>();
        User user = Common.getHttpUser();

        String existingDataSource = SystemSettingsDao.instance.getValue(SystemSettingsDao.PERMISSION_DATASOURCE);
        Permissions.validatePermissions(result, SystemSettingsDao.PERMISSION_DATASOURCE, user, false, Permissions.explodePermissionGroups(existingDataSource), Permissions.explodePermissionGroups(datasource));

        if(!result.getHasMessages())
            SystemSettingsDao.instance.setValue(SystemSettingsDao.PERMISSION_DATASOURCE, datasource);
        for (StringStringPair p : modulePermissions) {
            //Don't allow saving the superadmin permission as it doesn't do anything it's hard coded
            if(!p.getKey().equals(SuperadminPermissionDefinition.PERMISSION)) {
                ProcessResult partial = new ProcessResult();
                String existing = SystemSettingsDao.instance.getValue(p.getKey());
                Permissions.validatePermissions(partial, p.getKey(), user, false, Permissions.explodePermissionGroups(existing), Permissions.explodePermissionGroups(p.getValue()));

                if(!partial.getHasMessages())
                    SystemSettingsDao.instance.setValue(p.getKey(), p.getValue());
                else
                    messages.addAll(partial.getMessages());
            }
        }
        result.getMessages().addAll(messages);
        return result;
    }


    @DwrPermission(admin = true)
    public ProcessResult saveColourSettings(String chartBackgroundColour, String plotBackgroundColour,
            String plotGridlineColour) {
        ProcessResult response = new ProcessResult();

        try {
            ColorUtils.toColor(chartBackgroundColour);
        }
        catch (InvalidArgumentException e) {
            response.addContextualMessage(SystemSettingsDao.CHART_BACKGROUND_COLOUR,
                    "systemSettings.validation.invalidColour");
        }

        try {
            ColorUtils.toColor(plotBackgroundColour);
        }
        catch (InvalidArgumentException e) {
            response.addContextualMessage(SystemSettingsDao.PLOT_BACKGROUND_COLOUR,
                    "systemSettings.validation.invalidColour");
        }

        try {
            ColorUtils.toColor(plotGridlineColour);
        }
        catch (InvalidArgumentException e) {
            response.addContextualMessage(SystemSettingsDao.PLOT_GRIDLINE_COLOUR,
                    "systemSettings.validation.invalidColour");
        }

        if (!response.getHasMessages()) {
            SystemSettingsDao systemSettingsDao = SystemSettingsDao.instance;
            systemSettingsDao.setValue(SystemSettingsDao.CHART_BACKGROUND_COLOUR, chartBackgroundColour);
            systemSettingsDao.setValue(SystemSettingsDao.PLOT_BACKGROUND_COLOUR, plotBackgroundColour);
            systemSettingsDao.setValue(SystemSettingsDao.PLOT_GRIDLINE_COLOUR, plotGridlineColour);
        }

        return response;
    }

    @DwrPermission(admin = true)
    public void saveLanguageSettings(String language) {
        SystemSettingsDao.instance.setValue(SystemSettingsDao.LANGUAGE, language);
    }

    @DwrPermission(admin = true)
    public void purgeNow() {
        DataPurge dataPurge = new DataPurge();
        dataPurge.execute(Common.timer.currentTimeMillis());
    }

    @DwrPermission(admin = true)
    public TranslatableMessage purgeAllData() {
        boolean countPointValues = SystemSettingsDao.instance.getBooleanValue(SystemSettingsDao.POINT_DATA_PURGE_COUNT);
        if(countPointValues){
            long cnt = Common.runtimeManager.purgeDataPointValues();
            return new TranslatableMessage("systemSettings.purgeDataComplete", cnt);
        }else{
            Common.runtimeManager.purgeDataPointValuesWithoutCount();
            return new TranslatableMessage("systemSettings.purgeDataCompleteNoCount");
        }
    }

    @DwrPermission(admin = true)
    public TranslatableMessage purgeAllEvents() {
        int cnt = Common.eventManager.purgeAllEvents();
        return new TranslatableMessage("systemSettings.purgeAllEventsComplete", cnt);
    }

    @DwrPermission(admin = true)
    public void saveSettings(Map<String, String> settings) {
        SystemSettingsDao systemSettingsDao = SystemSettingsDao.instance;

        for (Map.Entry<String, String> entry : settings.entrySet())
            systemSettingsDao.setValue(entry.getKey(), entry.getValue());
    }

    /**
     * Save the Backup Settings to the DB.
     *
     * @param backupFileLocation
     * @param backupPeriod
     */
    @DwrPermission(admin = true)
    public ProcessResult saveBackupSettings(String backupFileLocation, int backupPeriodType, int backupPeriods,
            int backupHour, int backupMinute, int backupHistory, boolean backupEnabled) {
        ProcessResult result = new ProcessResult();

        SystemSettingsDao systemSettingsDao = SystemSettingsDao.instance;
        //Validate
        File tmp = new File(backupFileLocation);
        if (!tmp.exists()) {
            //Doesn't exist, push up message
            result.addContextualMessage(SystemSettingsDao.BACKUP_FILE_LOCATION,
                    "systemSettings.validation.backupLocationNotExists");
            return result;
        }
        if (!tmp.canWrite()) {
            result.addContextualMessage(SystemSettingsDao.BACKUP_FILE_LOCATION,
                    "systemSettings.validation.cannotWriteToBackupFileLocation");
            return result;
        }
        systemSettingsDao.setValue(SystemSettingsDao.BACKUP_FILE_LOCATION, backupFileLocation);

        //Not validating because select list.
        systemSettingsDao.setIntValue(SystemSettingsDao.BACKUP_PERIOD_TYPE, backupPeriodType);
        systemSettingsDao.setIntValue(SystemSettingsDao.BACKUP_PERIODS, backupPeriods);

        //Validate the Hour and Minute
        if ((backupHour < 24) && (backupHour >= 0)) {
            systemSettingsDao.setIntValue(SystemSettingsDao.BACKUP_HOUR, backupHour);
        }
        else {
            result.addContextualMessage(SystemSettingsDao.BACKUP_HOUR, "systemSettings.validation.backupHourInvalid");
        }
        if ((backupMinute < 60) && (backupMinute >= 0)) {
            systemSettingsDao.setIntValue(SystemSettingsDao.BACKUP_MINUTE, backupMinute);
        }
        else {
            result.addContextualMessage(SystemSettingsDao.BACKUP_MINUTE,
                    "systemSettings.validation.backupMinuteInvalid");
        }

        //Validate the number of backups to keep
        if (backupHistory > 0) {
            systemSettingsDao.setIntValue(SystemSettingsDao.BACKUP_FILE_COUNT, backupHistory);
        }
        else {
            result.addContextualMessage(SystemSettingsDao.BACKUP_FILE_COUNT,
                    "systemSettings.validation.backupFileCountInvalid");
        }

        systemSettingsDao.setBooleanValue(SystemSettingsDao.BACKUP_ENABLED, backupEnabled);

        return result;
    }

    /**
     * Queue a backup to run now.
     *
     * @param backupLocation
     */
    @DwrPermission(admin = true)
    public void queueBackup() {
        String backupLocation = SystemSettingsDao.instance.getValue(SystemSettingsDao.BACKUP_FILE_LOCATION);
        BackupWorkItem.queueBackup(backupLocation);
    }

    /**
     * Save the Backup Settings to the DB.
     *
     * @param backupFileLocation
     * @param backupPeriod
     */
    @DwrPermission(admin = true)
    public ProcessResult saveChartSettings(boolean allowAnonymousChartView, String chartFont) {
        SystemSettingsDao.instance.setBooleanValue(SystemSettingsDao.ALLOW_ANONYMOUS_CHART_VIEW, allowAnonymousChartView);
        if(!StringUtils.isEmpty(chartFont))
            SystemSettingsDao.instance.setValue(SystemSettingsDao.JFREE_CHART_FONT, chartFont);
        else
            SystemSettingsDao.instance.removeValue(SystemSettingsDao.JFREE_CHART_FONT);

        return new ProcessResult();
    }

    /**
     * Save the Backup Settings to the DB.
     *
     * @param backupFileLocation
     * @param backupPeriod
     */
    @DwrPermission(admin = true)
    public ProcessResult saveDatabaseBackupSettings(String backupFileLocation, int backupPeriodType, int backupPeriods,
            int backupHour, int backupMinute, int backupHistory, boolean backupEnabled) {
        ProcessResult result = new ProcessResult();

        SystemSettingsDao systemSettingsDao = SystemSettingsDao.instance;
        //Validate
        File tmp = new File(backupFileLocation);
        if (!tmp.exists()) {
            //Doesn't exist, push up message
            result.addContextualMessage(SystemSettingsDao.DATABASE_BACKUP_FILE_LOCATION,
                    "systemSettings.validation.backupLocationNotExists");
            return result;
        }
        if (!tmp.canWrite()) {
            result.addContextualMessage(SystemSettingsDao.DATABASE_BACKUP_FILE_LOCATION,
                    "systemSettings.validation.cannotWriteToBackupFileLocation");
            return result;
        }
        systemSettingsDao.setValue(SystemSettingsDao.DATABASE_BACKUP_FILE_LOCATION, backupFileLocation);

        //Not validating because select list.
        systemSettingsDao.setIntValue(SystemSettingsDao.DATABASE_BACKUP_PERIOD_TYPE, backupPeriodType);
        systemSettingsDao.setIntValue(SystemSettingsDao.DATABASE_BACKUP_PERIODS, backupPeriods);

        //Validate the Hour and Minute
        if ((backupHour < 24) && (backupHour >= 0)) {
            systemSettingsDao.setIntValue(SystemSettingsDao.DATABASE_BACKUP_HOUR, backupHour);
        }
        else {
            result.addContextualMessage(SystemSettingsDao.DATABASE_BACKUP_HOUR,
                    "systemSettings.validation.backupHourInvalid");
        }
        if ((backupMinute < 60) && (backupMinute >= 0)) {
            systemSettingsDao.setIntValue(SystemSettingsDao.DATABASE_BACKUP_MINUTE, backupMinute);
        }
        else {
            result.addContextualMessage(SystemSettingsDao.DATABASE_BACKUP_MINUTE,
                    "systemSettings.validation.backupMinuteInvalid");
        }

        //Validate the number of backups to keep
        if (backupHistory > 0) {
            systemSettingsDao.setIntValue(SystemSettingsDao.DATABASE_BACKUP_FILE_COUNT, backupHistory);
        }
        else {
            result.addContextualMessage(SystemSettingsDao.DATABASE_BACKUP_FILE_COUNT,
                    "systemSettings.validation.backupFileCountInvalid");
        }

        systemSettingsDao.setBooleanValue(SystemSettingsDao.DATABASE_BACKUP_ENABLED, backupEnabled);

        return result;
    }

    /**
     * Queue a backup to run now.
     *
     * @param backupLocation
     */
    @DwrPermission(admin = true)
    public void queueDatabaseBackup() {

        String backupLocation = SystemSettingsDao.instance.getValue(SystemSettingsDao.DATABASE_BACKUP_FILE_LOCATION);
        DatabaseBackupWorkItem.queueBackup(backupLocation);
    }

    /**
     * Queue a backup to run now.
     *
     * @param backupLocation
     */
    @DwrPermission(admin = true)
    public ProcessResult restoreDatabaseFromBackup(String file) {
        ProcessResult result = DatabaseBackupWorkItem.restore(file);
        return result;
    }

    /**
     * Queue a backup to run now.
     *
     * @param backupLocation
     */
    @DwrPermission(admin = true)
    public ProcessResult getDatabaseBackupFiles() {
        ProcessResult result = new ProcessResult();
        String backupLocation = SystemSettingsDao.instance.getValue(SystemSettingsDao.DATABASE_BACKUP_FILE_LOCATION);

        File[] backupFiles = DatabaseBackupWorkItem.getBackupFiles(backupLocation);
        if(backupFiles == null) {
            result.addContextualMessage(SystemSettingsDao.DATABASE_BACKUP_FILE_LOCATION, "systemSettings.validation.backupLocationNotExists");
            return result;
        }

        //Parse the list into data for a select list
        //Files of form core-database-TYPE-date_time
        List<String> filenames = new ArrayList<>();
        for (File file : backupFiles) {
            String filename = file.getName();
            filenames.add(filename);
        }
        result.addData("filenames", filenames);
        return result;
    }

    @DwrPermission(admin = true)
    public ProcessResult saveSerialSocketBridge(SerialSocketBridgeConfig config) {
        return saveVirtualSerialPortConfig(config);
    }

    @DwrPermission(admin = true)
    public ProcessResult saveSerialServerSocketBridge(SerialServerSocketBridgeConfig config) {
        return saveVirtualSerialPortConfig(config);
    }

    private ProcessResult saveVirtualSerialPortConfig(VirtualSerialPortConfig config) {
        ProcessResult response = new ProcessResult();

        //If we don't have a unique XID then we need to generate one
        if(StringUtils.isEmpty(config.getXid()))
            config.setXid(VirtualSerialPortConfigDao.getInstance().generateUniqueXid());

        config.validate(response);

        if(!response.getHasMessages()){
            response.addData("ports", VirtualSerialPortConfigDao.getInstance().save(config));
        }


        return response;
    }

    @DwrPermission(admin = true)
    public ProcessResult removeSerialSocketBridge(SerialSocketBridgeConfig config) {
        return removeVirtualSerialPortConfig(config);
    }

    @DwrPermission(admin = true)
    public ProcessResult removeSerialServerSocketBridge(SerialServerSocketBridgeConfig config) {
        return removeVirtualSerialPortConfig(config);
    }

    private ProcessResult removeVirtualSerialPortConfig(VirtualSerialPortConfig config) {
        ProcessResult response = new ProcessResult();
        response.addData("ports", VirtualSerialPortConfigDao.getInstance().remove(config));

        return response;
    }

}
