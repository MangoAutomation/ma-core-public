/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.db.dao;

import java.awt.Color;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import com.serotonin.InvalidArgumentException;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.spring.ExtendedJdbcTemplate;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.JsonWriter;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.vo.systemSettings.SystemSettingsEventDispatcher;
import com.serotonin.m2m2.vo.systemSettings.SystemSettingsVO;
import com.serotonin.util.ColorUtils;

public class SystemSettingsDao extends BaseDao {
    // Database schema version
    public static final String DATABASE_SCHEMA_VERSION = "databaseSchemaVersion";
    public static final String NEW_INSTANCE = "newInstance";

    // Email settings
    public static final String EMAIL_SMTP_HOST = "emailSmtpHost";
    public static final String EMAIL_SMTP_PORT = "emailSmtpPort";
    public static final String EMAIL_FROM_ADDRESS = "emailFromAddress";
    public static final String EMAIL_FROM_NAME = "emailFromName";
    public static final String EMAIL_AUTHORIZATION = "emailAuthorization";
    public static final String EMAIL_SMTP_USERNAME = "emailSmtpUsername";
    public static final String EMAIL_SMTP_PASSWORD = "emailSmtpPassword";
    public static final String EMAIL_TLS = "emailTls";
    public static final String EMAIL_CONTENT_TYPE = "emailContentType";

    // Point data purging
    public static final String POINT_DATA_PURGE_PERIOD_TYPE = "pointDataPurgePeriodType";
    public static final String POINT_DATA_PURGE_PERIODS = "pointDataPurgePeriods";

    // Event Type purging
    public static final String DATA_POINT_EVENT_PURGE_PERIOD_TYPE = "dataPointEventPurgePeriodType";
    public static final String DATA_POINT_EVENT_PURGE_PERIODS = "dataPointEventPurgePeriods";
    public static final String DATA_SOURCE_EVENT_PURGE_PERIOD_TYPE = "dataSourceEventPurgePeriodType";
    public static final String DATA_SOURCE_EVENT_PURGE_PERIODS = "dataSourceEventPurgePeriods";
    public static final String SYSTEM_EVENT_PURGE_PERIOD_TYPE = "systemEventPurgePeriodType";
    public static final String SYSTEM_EVENT_PURGE_PERIODS = "systemEventPurgePeriods";
    public static final String PUBLISHER_EVENT_PURGE_PERIOD_TYPE = "publisherEventPurgePeriodType";
    public static final String PUBLISHER_EVENT_PURGE_PERIODS = "publisherEventPurgePeriods";
    public static final String AUDIT_EVENT_PURGE_PERIOD_TYPE = "auditEventPurgePeriodType";
    public static final String AUDIT_EVENT_PURGE_PERIODS = "auditEventPurgePeriods";

    // Alarm Level Purging
    public static final String NONE_ALARM_PURGE_PERIOD_TYPE = "noneAlarmPurgePeriodType";
    public static final String NONE_ALARM_PURGE_PERIODS = "noneAlarmPurgePeriods";
    public static final String INFORMATION_ALARM_PURGE_PERIOD_TYPE = "informationAlarmPurgePeriodType";
    public static final String INFORMATION_ALARM_PURGE_PERIODS = "informationAlarmPurgePeriods";
    public static final String URGENT_ALARM_PURGE_PERIOD_TYPE = "urgentAlarmPurgePeriodType";
    public static final String URGENT_ALARM_PURGE_PERIODS = "urgentAlarmPurgePeriods";
    public static final String CRITICAL_ALARM_PURGE_PERIOD_TYPE = "criticalAlarmPurgePeriodType";
    public static final String CRITICAL_ALARM_PURGE_PERIODS = "criticalAlarmPurgePeriods";
    public static final String LIFE_SAFETY_ALARM_PURGE_PERIOD_TYPE = "lifeSafetyAlarmPurgePeriodType";
    public static final String LIFE_SAFETY_ALARM_PURGE_PERIODS = "lifeSafetyAlarmPurgePeriods";

    // General Purging of Events from Modules that are not defined in the core
    public static final String EVENT_PURGE_PERIOD_TYPE = "eventPurgePeriodType";
    public static final String EVENT_PURGE_PERIODS = "eventPurgePeriods";

    // HTTP Client configuration
    public static final String HTTP_CLIENT_USE_PROXY = "httpClientUseProxy";
    public static final String HTTP_CLIENT_PROXY_SERVER = "httpClientProxyServer";
    public static final String HTTP_CLIENT_PROXY_PORT = "httpClientProxyPort";
    public static final String HTTP_CLIENT_PROXY_USERNAME = "httpClientProxyUsername";
    public static final String HTTP_CLIENT_PROXY_PASSWORD = "httpClientProxyPassword";

    // i18n
    public static final String LANGUAGE = "language";

    // Customization
    public static final String FILEDATA_PATH = "filedata.path";
    public static final String DATASOURCE_DISPLAY_SUFFIX = ".display";
    public static final String HTTPDS_PROLOGUE = "httpdsPrologue";
    public static final String HTTPDS_EPILOGUE = "httpdsEpilogue";
    public static final String UI_PERFORMANCE = "uiPerformance";
    public static final String FUTURE_DATE_LIMIT_PERIODS = "futureDateLimitPeriods";
    public static final String FUTURE_DATE_LIMIT_PERIOD_TYPE = "futureDateLimitPeriodType";
    public static final String INSTANCE_DESCRIPTION = "instanceDescription";

    // Colours
    public static final String CHART_BACKGROUND_COLOUR = "chartBackgroundColour";
    public static final String PLOT_BACKGROUND_COLOUR = "plotBackgroundColour";
    public static final String PLOT_GRIDLINE_COLOUR = "plotGridlineColour";

    // Backup Settings
    public static final String BACKUP_FILE_LOCATION = "backupFileLocation";
    public static final String BACKUP_PERIOD_TYPE = "backupPeriodType";
    public static final String BACKUP_PERIODS = "backupPeriods";
    public static final String BACKUP_LAST_RUN_SUCCESS = "backupLastSuccessfulRun";
    public static final String BACKUP_HOUR = "backupHour";
    public static final String BACKUP_MINUTE = "backupMinute";
    public static final String BACKUP_FILE_COUNT = "backupFileCount";
    public static final String BACKUP_ENABLED = "backupEnabled";

    public static final String ALLOW_ANONYMOUS_CHART_VIEW = "allowAnonymousGraphicViews";

    public static final String DATABASE_BACKUP_FILE_LOCATION = "databaseBackupFileLocation";
    public static final String DATABASE_BACKUP_PERIOD_TYPE = "databaseBackupPeriodType";
    public static final String DATABASE_BACKUP_PERIODS = "databaseBackupPeriods";
    public static final String DATABASE_BACKUP_LAST_RUN_SUCCESS = "databaseBackupLastSuccessfulRun";
    public static final String DATABASE_BACKUP_HOUR = "databaseBackupHour";
    public static final String DATABASE_BACKUP_MINUTE = "databaseBackupMinute";
    public static final String DATABASE_BACKUP_FILE_COUNT = "databaseBackupFileCount";
    public static final String DATABASE_BACKUP_ENABLED = "databaseBackupEnabled";

    // Permissions
    public static final String PERMISSION_DATASOURCE = "permissionDatasource";
    
    //Background Processing
    public static final String HIGH_PRI_CORE_POOL_SIZE = "mediumPriorityThreadCorePoolSize";
    public static final String HIGH_PRI_MAX_POOL_SIZE = "mediumPriorityThreadMaximumPoolSize";
    
    public static final String MED_PRI_CORE_POOL_SIZE = "mediumPriorityThreadCorePoolSize";
    public static final String MED_PRI_MAX_POOL_SIZE = "mediumPriorityThreadMaximumPoolSize";
    
    public static final String LOW_PRI_CORE_POOL_SIZE = "lowPriorityThreadCorePoolSize";
    public static final String LOW_PRI_MAX_POOL_SIZE = "lowPriorityThreadMaximumPoolSize";

    // Value cache
    private static final Map<String, String> cache = new HashMap<>();

    public static String getValue(String key) {
        return getValue(key, (String) DEFAULT_VALUES.get(key));
    }

    public static String getValue(String key, String defaultValue) {
        String result = cache.get(key);
        if (result == null) {
            if (!cache.containsKey(key)) {
                result = new BaseDao().queryForObject("select settingValue from systemSettings where settingName=?",
                        new Object[] { key }, String.class, null);
                cache.put(key, result);
                if (result == null)
                    result = defaultValue;
            }
            else
                result = defaultValue;
        }
        return result;
    }

    public static int getIntValue(String key) {
        Integer defaultValue = (Integer) DEFAULT_VALUES.get(key);
        if (defaultValue == null)
            return getIntValue(key, 0);
        return getIntValue(key, defaultValue);
    }

    public static int getIntValue(String key, int defaultValue) {
        String value = getValue(key, null);
        if (value == null)
            return defaultValue;
        try {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static boolean getBooleanValue(String key) {
        return getBooleanValue(key, false);
    }

    public static boolean getBooleanValue(String key, boolean defaultValue) {
        String value = getValue(key, null);
        if (value == null)
            return defaultValue;
        return charToBool(value);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getJsonObject(String key, Class<T> clazz) {
        return (T) getJsonObject(key, (Type) clazz);
    }

    public static Object getJsonObject(String key, Type type) {
        String value = getValue(key, null);
        if (value == null)
            return null;
        try {
            return new JsonReader(Common.JSON_CONTEXT, value).read(type);
        }
        catch (Exception e) {
            // Things should only get here programmatically. Exceptions thrown here are for programmers to deal with.
            throw new RuntimeException(e);
        }
    }

    public void setValue(final String key, final String value) {
        String oldValue = cache.get(key);

        // Update the cache
        cache.put(key, value);

        // Update the database
        final ExtendedJdbcTemplate ejt2 = ejt;
        getTransactionTemplate().execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                // Delete any existing value.
                removeValue(key);

                // Insert the new value if it's not null.
                if (value != null)
                    ejt2.update("insert into systemSettings values (?,?)", new Object[] { key, value });
            }
        });

        //Fire an event for this
        SystemSettingsEventDispatcher.fireSystemSettingSaved(key, oldValue, value);
    }

    public void setIntValue(String key, int value) {
        setValue(key, Integer.toString(value));
    }

    public void setBooleanValue(String key, boolean value) {
        setValue(key, boolToChar(value));
    }

    public void setJsonObjectValue(String key, Object value) {
        try {
            setValue(key, JsonWriter.writeToString(Common.JSON_CONTEXT, value));
        }
        catch (JsonException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeValue(String key) {
        String lastValue = cache.get(key);

        // Remove the value from the cache
        cache.remove(key);

        // Reset the cached values too.
        FUTURE_DATE_LIMIT = -1;

        ejt.update("delete from systemSettings where settingName=?", new Object[] { key });

        //Fire the event
        SystemSettingsEventDispatcher.fireSystemSettingRemoved(key, lastValue);
    }

    public static long getFutureDateLimit() {
        if (FUTURE_DATE_LIMIT == -1)
            FUTURE_DATE_LIMIT = Common.getMillis(getIntValue(FUTURE_DATE_LIMIT_PERIOD_TYPE),
                    getIntValue(FUTURE_DATE_LIMIT_PERIODS));
        return FUTURE_DATE_LIMIT;
    }

    public static Color getColour(String key) {
        try {
            return ColorUtils.toColor(getValue(key));
        }
        catch (InvalidArgumentException e) {
            // Should never happen. Just use the default.
            try {
                return ColorUtils.toColor((String) DEFAULT_VALUES.get(key));
            }
            catch (InvalidArgumentException e1) {
                // This should definitely never happen
                throw new ShouldNeverHappenException(e1);
            }
        }
    }

    /**
     * Special caching for the future dated values property, which needs high performance.
     */
    private static long FUTURE_DATE_LIMIT = -1;

    private static final Map<String, Object> DEFAULT_VALUES = new HashMap<>();

    static {
        DEFAULT_VALUES.put(DATABASE_SCHEMA_VERSION, "0.7.0");

        DEFAULT_VALUES.put(HTTP_CLIENT_PROXY_SERVER, "");
        DEFAULT_VALUES.put(HTTP_CLIENT_PROXY_PORT, -1);
        DEFAULT_VALUES.put(HTTP_CLIENT_PROXY_USERNAME, "");
        DEFAULT_VALUES.put(HTTP_CLIENT_PROXY_PASSWORD, "");

        DEFAULT_VALUES.put(EMAIL_SMTP_HOST, "");
        DEFAULT_VALUES.put(EMAIL_SMTP_PORT, 25);
        DEFAULT_VALUES.put(EMAIL_FROM_ADDRESS, "");
        DEFAULT_VALUES.put(EMAIL_SMTP_USERNAME, "");
        DEFAULT_VALUES.put(EMAIL_SMTP_PASSWORD, "");
        DEFAULT_VALUES.put(EMAIL_FROM_NAME, "Mango Automation");

        DEFAULT_VALUES.put(POINT_DATA_PURGE_PERIOD_TYPE, Common.TimePeriods.YEARS);
        DEFAULT_VALUES.put(POINT_DATA_PURGE_PERIODS, 1);

        DEFAULT_VALUES.put(DATA_POINT_EVENT_PURGE_PERIOD_TYPE, Common.TimePeriods.YEARS);
        DEFAULT_VALUES.put(DATA_POINT_EVENT_PURGE_PERIODS, 1);
        
        DEFAULT_VALUES.put(DATA_SOURCE_EVENT_PURGE_PERIOD_TYPE, Common.TimePeriods.YEARS);
        DEFAULT_VALUES.put(DATA_SOURCE_EVENT_PURGE_PERIODS, 1);
        DEFAULT_VALUES.put(SYSTEM_EVENT_PURGE_PERIOD_TYPE, Common.TimePeriods.YEARS);
        DEFAULT_VALUES.put(SYSTEM_EVENT_PURGE_PERIODS, 1);
        DEFAULT_VALUES.put(PUBLISHER_EVENT_PURGE_PERIOD_TYPE, Common.TimePeriods.YEARS);
        DEFAULT_VALUES.put(PUBLISHER_EVENT_PURGE_PERIODS, 1);
        DEFAULT_VALUES.put(AUDIT_EVENT_PURGE_PERIOD_TYPE, Common.TimePeriods.YEARS);
        DEFAULT_VALUES.put(AUDIT_EVENT_PURGE_PERIODS, 1);

        DEFAULT_VALUES.put(NONE_ALARM_PURGE_PERIOD_TYPE, Common.TimePeriods.YEARS);
        DEFAULT_VALUES.put(NONE_ALARM_PURGE_PERIODS, 1);
        DEFAULT_VALUES.put(INFORMATION_ALARM_PURGE_PERIOD_TYPE, Common.TimePeriods.YEARS);
        DEFAULT_VALUES.put(INFORMATION_ALARM_PURGE_PERIODS, 1);
        DEFAULT_VALUES.put(URGENT_ALARM_PURGE_PERIOD_TYPE, Common.TimePeriods.YEARS);
        DEFAULT_VALUES.put(URGENT_ALARM_PURGE_PERIODS, 1);
        DEFAULT_VALUES.put(CRITICAL_ALARM_PURGE_PERIOD_TYPE, Common.TimePeriods.YEARS);
        DEFAULT_VALUES.put(CRITICAL_ALARM_PURGE_PERIODS, 1);
        DEFAULT_VALUES.put(LIFE_SAFETY_ALARM_PURGE_PERIOD_TYPE, Common.TimePeriods.YEARS);
        DEFAULT_VALUES.put(LIFE_SAFETY_ALARM_PURGE_PERIODS, 1);

        DEFAULT_VALUES.put(EVENT_PURGE_PERIOD_TYPE, Common.TimePeriods.YEARS);
        DEFAULT_VALUES.put(EVENT_PURGE_PERIODS, 1);

        DEFAULT_VALUES.put(LANGUAGE, "en");

        DEFAULT_VALUES.put(FILEDATA_PATH, "~/WEB-INF/filedata");
        DEFAULT_VALUES.put(HTTPDS_PROLOGUE, "");
        DEFAULT_VALUES.put(HTTPDS_EPILOGUE, "");
        DEFAULT_VALUES.put(UI_PERFORMANCE, 2000);
        DEFAULT_VALUES.put(FUTURE_DATE_LIMIT_PERIODS, 24);
        DEFAULT_VALUES.put(FUTURE_DATE_LIMIT_PERIOD_TYPE, Common.TimePeriods.HOURS);
        DEFAULT_VALUES.put(INSTANCE_DESCRIPTION, "My Mango Automation");

        DEFAULT_VALUES.put(CHART_BACKGROUND_COLOUR, "white");
        DEFAULT_VALUES.put(PLOT_BACKGROUND_COLOUR, "white");
        DEFAULT_VALUES.put(PLOT_GRIDLINE_COLOUR, "silver");

        //Default Backup Settings
        DEFAULT_VALUES.put(BACKUP_FILE_LOCATION, Common.MA_HOME + "/backup/");
        DEFAULT_VALUES.put(BACKUP_PERIOD_TYPE, Common.TimePeriods.DAYS); //Backup Daily
        DEFAULT_VALUES.put(BACKUP_PERIODS, 1);
        DEFAULT_VALUES.put(BACKUP_FILE_COUNT, 10);
        DEFAULT_VALUES.put(BACKUP_HOUR, 0);
        DEFAULT_VALUES.put(BACKUP_MINUTE, 5);
        //Can't use boolean here... DEFAULT_VALUES.put(BACKUP_ENABLED, true);
        //DEFAULT_VALUES.put(ALLOW_ANONYMOUS_GRAPHIC_VIEWS, false);
        DEFAULT_VALUES.put(DATABASE_BACKUP_FILE_LOCATION, Common.MA_HOME + "/backup/");
        DEFAULT_VALUES.put(DATABASE_BACKUP_PERIOD_TYPE, Common.TimePeriods.DAYS); //Backup Daily
        DEFAULT_VALUES.put(DATABASE_BACKUP_PERIODS, 1);
        DEFAULT_VALUES.put(DATABASE_BACKUP_FILE_COUNT, 10);
        DEFAULT_VALUES.put(DATABASE_BACKUP_HOUR, 0);
        DEFAULT_VALUES.put(DATABASE_BACKUP_MINUTE, 5);
        
        DEFAULT_VALUES.put(HIGH_PRI_CORE_POOL_SIZE, 0);   
        DEFAULT_VALUES.put(HIGH_PRI_MAX_POOL_SIZE, 100);   
        DEFAULT_VALUES.put(MED_PRI_CORE_POOL_SIZE, 3);   
        DEFAULT_VALUES.put(MED_PRI_MAX_POOL_SIZE, 30);   
        DEFAULT_VALUES.put(LOW_PRI_CORE_POOL_SIZE, 1);   
        DEFAULT_VALUES.put(LOW_PRI_MAX_POOL_SIZE, 1);   
        
    }

    /**
     * Get a VO that represents the cached values
     * 
     * @return
     */
    public SystemSettingsVO getSystemSettings() {
        SystemSettingsVO vo = new SystemSettingsVO();

        vo.setDatabaseSchemaVersion(getValue(DATABASE_SCHEMA_VERSION));
        vo.setNewInstance(getBooleanValue(NEW_INSTANCE));

        vo.setEmailSmtpHost(getValue(EMAIL_SMTP_HOST));
        vo.setEmailSmtpPort(getIntValue(EMAIL_SMTP_PORT));
        vo.setEmailFromAddress(getValue(EMAIL_FROM_ADDRESS));
        vo.setEmailSmtpUsername(getValue(EMAIL_SMTP_USERNAME));
        vo.setEmailSmtpPassword(getValue(EMAIL_SMTP_PASSWORD));
        vo.setEmailFromName(getValue(EMAIL_FROM_NAME));
        vo.setEmailAuthorization(getBooleanValue(EMAIL_AUTHORIZATION));
        vo.setEmailTls(getBooleanValue(EMAIL_TLS));
        vo.setEmailContentType(getIntValue(EMAIL_CONTENT_TYPE));

        vo.setPointDataPurgePeriodType(getIntValue(POINT_DATA_PURGE_PERIOD_TYPE));
        vo.setPointDataPurgePeriods(getIntValue(POINT_DATA_PURGE_PERIODS));

        vo.setDataPointEventPurgePeriodType(getIntValue(DATA_POINT_EVENT_PURGE_PERIOD_TYPE));
        vo.setDataPointEventPurgePeriods(getIntValue(DATA_POINT_EVENT_PURGE_PERIODS));
        vo.setDataSourceEventPurgePeriodType(getIntValue(DATA_SOURCE_EVENT_PURGE_PERIOD_TYPE));
        vo.setDataSourceEventPurgePeriods(getIntValue(DATA_SOURCE_EVENT_PURGE_PERIODS));
        vo.setSystemEventPurgePeriodType(getIntValue(SYSTEM_EVENT_PURGE_PERIOD_TYPE));
        vo.setSystemEventPurgePeriods(getIntValue(SYSTEM_EVENT_PURGE_PERIODS));
        vo.setPublisherEventPurgePeriodType(getIntValue(PUBLISHER_EVENT_PURGE_PERIOD_TYPE));
        vo.setPublisherEventPurgePeriods(getIntValue(PUBLISHER_EVENT_PURGE_PERIODS));
        vo.setAuditEventPurgePeriodType(getIntValue(AUDIT_EVENT_PURGE_PERIOD_TYPE));
        vo.setAuditEventPurgePeriods(getIntValue(AUDIT_EVENT_PURGE_PERIODS));

        vo.setNoneAlarmPurgePeriodType(getIntValue(NONE_ALARM_PURGE_PERIOD_TYPE));
        vo.setNoneAlarmPurgePeriods(getIntValue(NONE_ALARM_PURGE_PERIODS));
        vo.setInformationAlarmPurgePeriodType(getIntValue(INFORMATION_ALARM_PURGE_PERIOD_TYPE));
        vo.setInformationAlarmPurgePeriods(getIntValue(INFORMATION_ALARM_PURGE_PERIODS));
        vo.setUrgentAlarmPurgePeriodType(getIntValue(URGENT_ALARM_PURGE_PERIOD_TYPE));
        vo.setUrgentAlarmPurgePeriods(getIntValue(URGENT_ALARM_PURGE_PERIODS));
        vo.setCriticalAlarmPurgePeriodType(getIntValue(CRITICAL_ALARM_PURGE_PERIOD_TYPE));
        vo.setCriticalAlarmPurgePeriods(getIntValue(CRITICAL_ALARM_PURGE_PERIODS));
        vo.setLifeSafetyAlarmPurgePeriodType(getIntValue(LIFE_SAFETY_ALARM_PURGE_PERIOD_TYPE));
        vo.setLifeSafetyAlarmPurgePeriods(getIntValue(LIFE_SAFETY_ALARM_PURGE_PERIODS));

        vo.setEventPurgePeriodType(getIntValue(EVENT_PURGE_PERIOD_TYPE));
        vo.setEventPurgePeriods(getIntValue(EVENT_PURGE_PERIODS));

        vo.setHttpClientUseProxy(getBooleanValue(HTTP_CLIENT_USE_PROXY));
        vo.setHttpClientProxyServer(getValue(HTTP_CLIENT_PROXY_SERVER));
        vo.setHttpClientProxyPort(getIntValue(HTTP_CLIENT_PROXY_PORT));
        vo.setHttpClientProxyUsername(getValue(HTTP_CLIENT_PROXY_USERNAME));
        vo.setHttpClientProxyPassword(getValue(HTTP_CLIENT_PROXY_USERNAME));

        vo.setLanguage(getValue(LANGUAGE));

        vo.setFiledataPath(getValue(FILEDATA_PATH));
        vo.setDatasourceDisplaySuffix(getValue(DATASOURCE_DISPLAY_SUFFIX));
        vo.setHttpdsPrologue(getValue(HTTPDS_PROLOGUE));
        vo.setHttpdsEpilogue(getValue(HTTPDS_EPILOGUE));
        vo.setUiPerformance(getIntValue(UI_PERFORMANCE));
        vo.setFutureDateLimitPeriods(getIntValue(FUTURE_DATE_LIMIT_PERIODS));
        vo.setFutureDateLimitPeriodType(getIntValue(FUTURE_DATE_LIMIT_PERIOD_TYPE));
        vo.setInstanceDescription(getValue(INSTANCE_DESCRIPTION));

        vo.setChartBackgroundColor(getValue(CHART_BACKGROUND_COLOUR));
        vo.setPlotBackgroundColor(getValue(PLOT_BACKGROUND_COLOUR));
        vo.setPlotGridlineColor(getValue(PLOT_GRIDLINE_COLOUR));

        vo.setBackupFileLocation(getValue(BACKUP_FILE_LOCATION));
        vo.setBackupPeriodType(getIntValue(BACKUP_PERIOD_TYPE));
        vo.setBackupPeriods(getIntValue(BACKUP_PERIODS));
        vo.setBackupLastRunSuccess(getValue(BACKUP_LAST_RUN_SUCCESS));
        vo.setBackupFileCount(getIntValue(BACKUP_FILE_COUNT));
        vo.setBackupHour(getIntValue(BACKUP_HOUR));
        vo.setBackupMinute(getIntValue(BACKUP_MINUTE));
        vo.setBackupEnabled(getBooleanValue(BACKUP_ENABLED));

        vo.setAllowAnonymousChartView(getBooleanValue(ALLOW_ANONYMOUS_CHART_VIEW));

        vo.setDatabaseBackupFileLocation(getValue(DATABASE_BACKUP_FILE_LOCATION));
        vo.setDatabaseBackupPeriodType(getIntValue(DATABASE_BACKUP_PERIOD_TYPE));
        vo.setDatabaseBackupPeriods(getIntValue(DATABASE_BACKUP_PERIODS));
        vo.setDatabaseBackupLastRunSuccess(getValue(DATABASE_BACKUP_LAST_RUN_SUCCESS));
        vo.setDatabaseBackupFileCount(getIntValue(DATABASE_BACKUP_FILE_COUNT));
        vo.setDatabaseBackupHour(getIntValue(DATABASE_BACKUP_HOUR));
        vo.setDatabaseBackupMinute(getIntValue(DATABASE_BACKUP_MINUTE));
        vo.setDatabaseBackupEnabled(getBooleanValue(DATABASE_BACKUP_ENABLED));

        return vo;
    }

    /**
     * Take a VO and save its values to the system
     * 
     * @param vo
     */
    public void updateSettings(SystemSettingsVO vo) {

        this.setValue(DATABASE_SCHEMA_VERSION, vo.getDatabaseSchemaVersion());
        this.setBooleanValue(NEW_INSTANCE, vo.getNewInstance());

        this.setValue(EMAIL_SMTP_HOST, vo.getEmailSmtpHost());
        this.setIntValue(EMAIL_SMTP_PORT, vo.getEmailSmtpPort());
        this.setValue(EMAIL_FROM_ADDRESS, vo.getEmailFromAddress());
        this.setValue(EMAIL_SMTP_USERNAME, vo.getEmailSmtpUsername());
        this.setValue(EMAIL_SMTP_PASSWORD, vo.getEmailSmtpPassword());
        this.setValue(EMAIL_FROM_NAME, vo.getEmailFromName());
        this.setBooleanValue(EMAIL_AUTHORIZATION, vo.getEmailAuthorization());
        this.setBooleanValue(EMAIL_TLS, vo.getEmailTls());
        this.setIntValue(EMAIL_CONTENT_TYPE, vo.getEmailContentType());

        this.setIntValue(DATA_POINT_EVENT_PURGE_PERIOD_TYPE, vo.getDataPointEventPurgePeriodType());
        this.setIntValue(DATA_POINT_EVENT_PURGE_PERIODS, vo.getDataPointEventPurgePeriods());

        this.setIntValue(POINT_DATA_PURGE_PERIOD_TYPE, vo.getPointDataPurgePeriodType());
        this.setIntValue(POINT_DATA_PURGE_PERIODS, vo.getPointDataPurgePeriods());

        this.setIntValue(DATA_POINT_EVENT_PURGE_PERIOD_TYPE, vo.getDataPointEventPurgePeriodType());
        this.setIntValue(DATA_POINT_EVENT_PURGE_PERIODS, vo.getDataPointEventPurgePeriods());
        this.setIntValue(DATA_SOURCE_EVENT_PURGE_PERIOD_TYPE, vo.getDataSourceEventPurgePeriodType());
        this.setIntValue(DATA_SOURCE_EVENT_PURGE_PERIODS, vo.getDataSourceEventPurgePeriods());
        this.setIntValue(SYSTEM_EVENT_PURGE_PERIOD_TYPE, vo.getSystemEventPurgePeriodType());
        this.setIntValue(SYSTEM_EVENT_PURGE_PERIODS, vo.getSystemEventPurgePeriods());
        this.setIntValue(PUBLISHER_EVENT_PURGE_PERIOD_TYPE, vo.getPublisherEventPurgePeriodType());
        this.setIntValue(PUBLISHER_EVENT_PURGE_PERIODS, vo.getPublisherEventPurgePeriods());
        this.setIntValue(AUDIT_EVENT_PURGE_PERIOD_TYPE, vo.getAuditEventPurgePeriodType());
        this.setIntValue(AUDIT_EVENT_PURGE_PERIODS, vo.getAuditEventPurgePeriods());

        this.setIntValue(NONE_ALARM_PURGE_PERIOD_TYPE, vo.getNoneAlarmPurgePeriodType());
        this.setIntValue(NONE_ALARM_PURGE_PERIODS, vo.getNoneAlarmPurgePeriods());
        this.setIntValue(INFORMATION_ALARM_PURGE_PERIOD_TYPE, vo.getInformationAlarmPurgePeriodType());
        this.setIntValue(INFORMATION_ALARM_PURGE_PERIODS, vo.getInformationAlarmPurgePeriods());
        this.setIntValue(URGENT_ALARM_PURGE_PERIOD_TYPE, vo.getUrgentAlarmPurgePeriodType());
        this.setIntValue(URGENT_ALARM_PURGE_PERIODS, vo.getUrgentAlarmPurgePeriods());
        this.setIntValue(CRITICAL_ALARM_PURGE_PERIOD_TYPE, vo.getCriticalAlarmPurgePeriodType());
        this.setIntValue(CRITICAL_ALARM_PURGE_PERIODS, vo.getCriticalAlarmPurgePeriods());
        this.setIntValue(LIFE_SAFETY_ALARM_PURGE_PERIOD_TYPE, vo.getLifeSafetyAlarmPurgePeriodType());
        this.setIntValue(LIFE_SAFETY_ALARM_PURGE_PERIODS, vo.getLifeSafetyAlarmPurgePeriods());

        this.setIntValue(EVENT_PURGE_PERIOD_TYPE, vo.getEventPurgePeriodType());
        this.setIntValue(EVENT_PURGE_PERIODS, vo.getEventPurgePeriods());

        this.setBooleanValue(HTTP_CLIENT_USE_PROXY, vo.getHttpClientUseProxy());
        this.setValue(HTTP_CLIENT_PROXY_SERVER, vo.getHttpClientProxyServer());
        this.setIntValue(HTTP_CLIENT_PROXY_PORT, vo.getHttpClientProxyPort());
        this.setValue(HTTP_CLIENT_PROXY_USERNAME, vo.getHttpClientProxyUsername());
        this.setValue(HTTP_CLIENT_PROXY_PASSWORD, vo.getHttpClientProxyPassword());

        this.setValue(LANGUAGE, vo.getLanguage());

        this.setValue(FILEDATA_PATH, vo.getFiledataPath());
        this.setValue(DATASOURCE_DISPLAY_SUFFIX, vo.getDatasourceDisplaySuffix());
        this.setValue(HTTPDS_PROLOGUE, vo.getHttpdsPrologue());
        this.setValue(HTTPDS_EPILOGUE, vo.getHttpdsEpilogue());
        this.setIntValue(UI_PERFORMANCE, vo.getUiPerformance());
        this.setIntValue(FUTURE_DATE_LIMIT_PERIODS, vo.getFutureDateLimitPeriods());
        this.setIntValue(FUTURE_DATE_LIMIT_PERIOD_TYPE, vo.getFutureDateLimitPeriodType());
        this.setValue(INSTANCE_DESCRIPTION, vo.getInstanceDescription());

        this.setValue(CHART_BACKGROUND_COLOUR, vo.getChartBackgroundColor());
        this.setValue(PLOT_BACKGROUND_COLOUR, vo.getChartBackgroundColor());
        this.setValue(PLOT_GRIDLINE_COLOUR, vo.getPlotGridlineColor());

        this.setValue(BACKUP_FILE_LOCATION, vo.getBackupFileLocation());
        this.setIntValue(BACKUP_PERIOD_TYPE, vo.getBackupPeriodType());
        this.setIntValue(BACKUP_PERIODS, vo.getBackupPeriods());
        this.setValue(BACKUP_LAST_RUN_SUCCESS, vo.getBackupLastRunSuccess());
        this.setIntValue(BACKUP_FILE_COUNT, vo.getBackupFileCount());
        this.setIntValue(BACKUP_HOUR, vo.getBackupHour());
        this.setIntValue(BACKUP_MINUTE, vo.getBackupMinute());
        this.setBooleanValue(BACKUP_ENABLED, vo.getBackupEnabled());

        this.setBooleanValue(ALLOW_ANONYMOUS_CHART_VIEW, vo.getAllowAnonymousChartView());

        this.setValue(DATABASE_BACKUP_FILE_LOCATION, vo.getDatabaseBackupFileLocation());
        this.setIntValue(DATABASE_BACKUP_PERIOD_TYPE, vo.getDatabaseBackupPeriodType());
        this.setIntValue(DATABASE_BACKUP_PERIODS, vo.getDatabaseBackupPeriods());
        this.setValue(DATABASE_BACKUP_LAST_RUN_SUCCESS, vo.getDatabaseBackupLastRunSuccess());
        this.setIntValue(DATABASE_BACKUP_FILE_COUNT, vo.getDatabaseBackupFileCount());
        this.setIntValue(DATABASE_BACKUP_HOUR, vo.getDatabaseBackupHour());
        this.setIntValue(DATABASE_BACKUP_MINUTE, vo.getDatabaseBackupMinute());
        this.setBooleanValue(DATABASE_BACKUP_ENABLED, vo.getDatabaseBackupEnabled());

    }
}
