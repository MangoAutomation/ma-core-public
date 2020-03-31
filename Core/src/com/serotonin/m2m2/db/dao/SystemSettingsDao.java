/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.db.dao;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.infiniteautomation.mango.spring.MangoRuntimeContextConfiguration;
import com.infiniteautomation.mango.spring.components.EmailAddressVerificationService;
import com.infiniteautomation.mango.spring.components.PasswordResetService;
import com.serotonin.InvalidArgumentException;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.spring.ExtendedJdbcTemplate;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.JsonWriter;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.Common.TimePeriods;
import com.serotonin.m2m2.UpgradeVersionState;
import com.serotonin.m2m2.db.DatabaseProxy.DatabaseType;
import com.serotonin.m2m2.email.MangoEmailContent;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.module.AuditEventTypeDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.SystemEventTypeDefinition;
import com.serotonin.m2m2.module.SystemSettingsDefinition;
import com.serotonin.m2m2.module.definitions.permissions.SuperadminPermissionDefinition;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.rt.event.type.SystemEventType;
import com.serotonin.m2m2.rt.maint.BackgroundProcessing;
import com.serotonin.m2m2.rt.maint.DataPurge;
import com.serotonin.m2m2.util.ColorUtils;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.systemSettings.SystemSettingsEventDispatcher;

/**
 * Dao access for system settings,
 *  this class is a spring bean declared in the MangoRuntimeContextConfiguration,
 *  as such it is not annotated as a bean here.
 *
 * @author Jared Wiltshire
 */
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
    public static final String EMAIL_SEND_TIMEOUT = "emailSendTimeout";
    public static final String EMAIL_DISABLED = "emailDisabled";

    // Base URL to use when inserting links in emails etc
    public static final String PUBLICLY_RESOLVABLE_BASE_URL = "publiclyResolvableBaseUrl";
    // Used if PUBLICLY_RESOLVABLE_BASE_URL is not set
    public static final String PUBLIC_HOSTNAME = "publicHostname";

    // Point data purging
    public static final String POINT_DATA_PURGE_PERIOD_TYPE = "pointDataPurgePeriodType";
    public static final String POINT_DATA_PURGE_PERIODS = "pointDataPurgePeriods";
    public static final String POINT_DATA_PURGE_COUNT = "pointDataPurgeCount";

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
    public static final String IMPORTANT_ALARM_PURGE_PERIOD_TYPE = "importantAlarmPurgePeriodType";
    public static final String IMPORTANT_ALARM_PURGE_PERIODS = "importantAlarmPurgePeriods";
    public static final String WARNING_ALARM_PURGE_PERIOD_TYPE = "warningAlarmPurgePeriodType";
    public static final String WARNING_ALARM_PURGE_PERIODS = "warningAlarmPurgePeriods";
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
    public static final String DATASOURCE_DISPLAY_SUFFIX = ".display";
    public static final String HTTPDS_PROLOGUE = "httpdsPrologue";
    public static final String HTTPDS_EPILOGUE = "httpdsEpilogue";
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

    // Chart API settings, null defaults
    public static final String ALLOW_ANONYMOUS_CHART_VIEW = "allowAnonymousGraphicViews";
    public static final String JFREE_CHART_FONT = "jfreeChartFont";

    public static final String DATABASE_BACKUP_FILE_LOCATION = "databaseBackupFileLocation";
    public static final String DATABASE_BACKUP_PERIOD_TYPE = "databaseBackupPeriodType";
    public static final String DATABASE_BACKUP_PERIODS = "databaseBackupPeriods";
    public static final String DATABASE_BACKUP_LAST_RUN_SUCCESS = "databaseBackupLastSuccessfulRun";
    public static final String DATABASE_BACKUP_HOUR = "databaseBackupHour";
    public static final String DATABASE_BACKUP_MINUTE = "databaseBackupMinute";
    public static final String DATABASE_BACKUP_FILE_COUNT = "databaseBackupFileCount";
    public static final String DATABASE_BACKUP_ENABLED = "databaseBackupEnabled";

    public static final String RESTART_DELAY = "restart.delay";

    //Background Processing
    public static final String HIGH_PRI_CORE_POOL_SIZE = "highPriorityThreadCorePoolSize";
    public static final String HIGH_PRI_MAX_POOL_SIZE = "highPriorityThreadMaximumPoolSize";

    public static final String MED_PRI_CORE_POOL_SIZE = "mediumPriorityThreadCorePoolSize";

    public static final String LOW_PRI_CORE_POOL_SIZE = "lowPriorityThreadCorePoolSize";

    //Site analytics
    public static final String SITE_ANALYTICS_HEAD = "siteAnalyticsHead";
    public static final String SITE_ANALYTICS_BODY = "siteAnalyticsBody";

    //Download update settings
    public static final String UPGRADE_VERSION_STATE = "upgradeVersionState";

    // Last install/upgrade of Mango Core or modules
    public static final String LAST_UPGRADE = "lastUpgrade";
    public static final String CORE_VERSION_LAST_START = "coreVersionLastStart";

    // Timeouts for http sessions
    public static final String HTTP_SESSION_TIMEOUT_PERIOD_TYPE = "httpSessionTimeoutPeriodType";
    public static final String HTTP_SESSION_TIMEOUT_PERIODS = "httpSessionTimeoutPeriods";

    // Password expiration settings
    public static final String PASSWORD_EXPIRATION_ENABLED = "passwordExpirationEnabled";
    public static final String PASSWORD_EXPIRATION_PERIOD_TYPE = "passwordExpirationPeriodType";
    public static final String PASSWORD_EXPIRATION_PERIODS = "passwordExpirationPeriods";

    //Usage tracking statistics uploading
    public static final String USAGE_TRACKING_ENABLED = "usageTrackingEnabled";

    //Check Store for Upgrades
    public static final String UPGRADE_CHECKS_ENABLED = "upgradeChecksEnabled";

    //License Agreement Acceptance Tracking
    public static final String LICENSE_AGREEMENT_VERSION = "licenseAgreementVersion";

    public static final String USERS_PUBLIC_REGISTRATION_ENABLED = "users.publicRegistration.enabled";

    //Password rules settings, count of 0 means no rule applied
    public static final String PASSWORD_UPPER_CASE_COUNT = "password.rule.upperCaseCount";
    public static final String PASSWORD_LOWER_CASE_COUNT = "password.rule.lowerCaseCount";
    public static final String PASSWORD_DIGIT_COUNT = "password.rule.digitCount";
    public static final String PASSWORD_SPECIAL_COUNT = "password.rule.specialCount";
    public static final String PASSWORD_LENGTH_MIN = "password.rule.lengthMin";
    public static final String PASSWORD_LENGTH_MAX = "password.rule.lengthMax";
    //TODO Dictionary
    //TODO Validation

    public static final SystemSettingsDao instance = new SystemSettingsDao();

    /**
     * Will remain null until the runtime context is refreshed so the JSON methods of this class should not be used early in the lifecycle.
     */
    @Autowired
    @Qualifier(MangoRuntimeContextConfiguration.DAO_OBJECT_MAPPER_NAME)
    private final ObjectMapper mapper = null;

    private SystemSettingsDao() {
    }

    // Value cache
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public String getValue(String key) {
        Object defaultValue = DEFAULT_VALUES.get(key);
        if(defaultValue == null){
            return getValue(key, null);
        }else{
            if(defaultValue instanceof String)
                return getValue(key, (String)defaultValue);
            else if(defaultValue instanceof Integer)
                return getValue(key, ((Integer)defaultValue).toString());
            else if(defaultValue instanceof Boolean)
                return getValue(key, ((Boolean)defaultValue).toString());
            else
                throw new ShouldNeverHappenException("Unsupported type of default value " + defaultValue.getClass().getCanonicalName() + " for System Setting: " + key);
        }

    }

    @Deprecated
    public String getValue(String key, String defaultValue) {
        String result = cache.computeIfAbsent(key, (k) -> {
            return new BaseDao().queryForObject("select settingValue from systemSettings where settingName=?",
                    new Object[] { k }, String.class, null);
        });

        if (result == null) {
            result = defaultValue;
        }

        return result;
    }

    public int getIntValue(String key) {
        Integer defaultValue = (Integer) DEFAULT_VALUES.get(key);
        if (defaultValue == null)
            return getIntValue(key, 0);
        return getIntValue(key, defaultValue);
    }

    @Deprecated
    public int getIntValue(String key, int defaultValue) {
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

    public boolean getBooleanValue(String key) {
        Boolean defaultValue = (Boolean) DEFAULT_VALUES.get(key);
        if (defaultValue == null)
            return getBooleanValue(key, false);
        return getBooleanValue(key, defaultValue);
    }

    @Deprecated
    public boolean getBooleanValue(String key, boolean defaultValue) {
        String value = getValue(key, null);
        if (value == null)
            return defaultValue;
        return charToBool(value);
    }

    /**
     * This method uses Serotonin JSON deserialization. Prefer {@link #getAsJson(String, TypeReference)}
     * @param key
     * @param type
     * @return
     */
    @Deprecated
    public Object getJsonObject(String key, Type type) {
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

    /**
     * Use the Jackson DAO mapper to serialize the object to JSON
     * @param key
     * @param value
     */
    public void setAsJson(String key, Object value) {
        try {
            this.setValue(key, mapper.writeValueAsString(value));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Useful for validating JSON system settings where the value could be a JsonNode (if from REST) or String (if importing system setting)
     * @param value
     * @return
     */
    public JsonNode readAsJson(Object value) {
        try {
            if (value instanceof JsonNode) {
                return (JsonNode) value;
            } else if (value instanceof String) {
                return mapper.reader().readTree((String) value);
            } else {
                throw new IllegalArgumentException("Value must be String or JsonNode");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Useful for validating JSON system settings where the value could be a JsonNode (if from REST) or String (if importing system setting)
     * @param value
     * @param clazz
     * @return
     */
    public <T> T readAsJson(Object value, Class<T> clazz) {
        try {
            ObjectMapper mapper = this.mapper;
            if (value instanceof JsonNode) {
                JsonParser parser = mapper.treeAsTokens((JsonNode) value);
                return mapper.readValue(parser, clazz);
            } else if (value instanceof String) {
                return mapper.readValue((String) value, clazz);
            } else {
                throw new IllegalArgumentException("Value must be String or JsonNode");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Useful for validating JSON system settings where the value could be a JsonNode (if from REST) or String (if importing system setting).
     *
     *
     * <p>Example usage</p>
     * <pre>
     * MyClass&lt;String&gt; result = readAsJson(value, new TypeReference&lt;MyClass&lt;String&gt;&gt;() {});
     * </pre>
     *
     * @param value
     * @param typeReference
     * @return
     */
    public <T> T readAsJson(Object value, TypeReference<T> typeReference) {
        try {
            ObjectMapper mapper = this.mapper;
            if (value instanceof JsonNode) {
                JsonParser parser = mapper.treeAsTokens((JsonNode) value);
                return mapper.readValue(parser, typeReference);
            } else if (value instanceof String) {
                return mapper.readValue((String) value, typeReference);
            } else {
                throw new IllegalArgumentException("Value must be String or JsonNode");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Useful for validating JSON system settings where the value could be a JsonNode (if from REST) or String (if importing system setting)
     * @param value
     * @param collectionClazz
     * @param clazz
     * @return
     */
    public <T> T readAsJsonCollection(Object value, @SuppressWarnings("rawtypes") Class<? extends Collection> collectionClazz, Class<?> clazz) {
        ObjectMapper mapper = this.mapper;
        CollectionType type = mapper.getTypeFactory().constructCollectionType(collectionClazz, clazz);

        try {
            if (value instanceof JsonNode) {
                JsonParser parser = mapper.treeAsTokens((JsonNode) value);
                return mapper.readValue(parser, type);
            } else if (value instanceof String) {
                return mapper.readValue((String) value, type);
            } else {
                throw new IllegalArgumentException("Value must be String or JsonNode");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieve the system setting as a JSON node using the Jackson DAO mapper
     * @param key
     * @return
     */
    public JsonNode getAsJson(String key) {
        String value = this.getValue(key);
        if (value == null) {
            return null;
        }
        return readAsJson(value);
    }

    /**
     * Retrieve the system setting as a JSON node using the Jackson DAO mapper
     * @param key
     * @param clazz
     * @return
     */
    public <T> T getAsJson(String key, Class<T> clazz) {
        String value = this.getValue(key);
        if (value == null) {
            return null;
        }
        return readAsJson(value, clazz);
    }

    /**
     * Retrieve the system setting as a JSON node using the Jackson DAO mapper
     * @param key
     * @param typeReference
     * @return
     */
    public <T> T getAsJson(String key, TypeReference<T> typeReference) {
        String value = this.getValue(key);
        if (value == null) {
            return null;
        }
        return readAsJson(value, typeReference);
    }

    public <T> T getAsJsonCollection(String key, @SuppressWarnings("rawtypes") Class<? extends Collection> collectionClazz, Class<?> clazz) {
        String value = this.getValue(key);
        if (value == null) {
            return null;
        }
        return readAsJsonCollection(value, collectionClazz, clazz);
    }

    public void setValue(final String key, final String value) {
        // Update the cache
        String oldValue;
        if (value == null) {
            oldValue = cache.remove(key);
        } else {
            oldValue = cache.put(key, value);
        }

        // Update the database
        final ExtendedJdbcTemplate ejt2 = ejt;
        getTransactionTemplate().execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                //There is potential deadlock in MySQL here, this avoids it
                if(Common.databaseProxy.getType() == DatabaseType.MYSQL) {
                    // Delete any existing value.
                    if(value == null) {
                        removeValue(key);
                    }else {
                        ejt2.update("insert into systemSettings values (?,?) on duplicate key update settingValue=?", new Object[] { key, value, value });
                    }
                }else {
                    // Delete any existing value.
                    removeValue(key);

                    // Insert the new value if it's not null.
                    if (value != null)
                        ejt2.update("insert into systemSettings values (?,?)", new Object[] { key, value });
                }
            }
        });

        this.updateThreadPoolSettings(key, value);
        SystemSettingsEventDispatcher.INSTANCE.fireSystemSettingSaved(key, oldValue, value);
    }

    /**
     * We would usually use a SystemSettingsListener for this but if the pools are full we may not be able to spawn a thread to update Common.backgroundProcessing
     * @param key
     * @param value
     */
    private void updateThreadPoolSettings(String key, String value) {
        switch (key) {
            case HIGH_PRI_CORE_POOL_SIZE:
                Common.backgroundProcessing.setHighPriorityServiceCorePoolSize(Integer.parseInt(value));
                break;
            case HIGH_PRI_MAX_POOL_SIZE:
                Common.backgroundProcessing.setHighPriorityServiceMaximumPoolSize(Integer.parseInt(value));
                break;
            case MED_PRI_CORE_POOL_SIZE:
                Common.backgroundProcessing.setMediumPriorityServiceCorePoolSize(Integer.parseInt(value));
                break;
            case LOW_PRI_CORE_POOL_SIZE:
                Common.backgroundProcessing.setLowPriorityServiceCorePoolSize(Integer.parseInt(value));
                break;
        }
    }

    public void setIntValue(String key, int value) {
        setValue(key, Integer.toString(value));
    }

    public void setBooleanValue(String key, boolean value) {
        setValue(key, boolToChar(value));
    }

    /**
     * This method uses Serotonin JSON serialization. Prefer {@link #setAsJson(String, Object)}
     * @param key
     * @param value
     */
    @Deprecated
    public void setJsonObjectValue(String key, Object value) {
        try {
            setValue(key, JsonWriter.writeToString(Common.JSON_CONTEXT, value));
        }
        catch (JsonException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeValue(String key) {
        // Remove the value from the cache
        String lastValue = cache.remove(key);

        // Reset the cached values too.
        FUTURE_DATE_LIMIT = -1;

        ejt.update("delete from systemSettings where settingName=?", new Object[] { key });

        //Fire the event
        SystemSettingsEventDispatcher.INSTANCE.fireSystemSettingRemoved(key, lastValue, getValue(key));
    }

    public long getFutureDateLimit() {
        if (FUTURE_DATE_LIMIT == -1)
            FUTURE_DATE_LIMIT = Common.getMillis(instance.getIntValue(FUTURE_DATE_LIMIT_PERIOD_TYPE),
                    instance.getIntValue(FUTURE_DATE_LIMIT_PERIODS));
        return FUTURE_DATE_LIMIT;
    }

    public Color getColour(String key) {
        try {
            return ColorUtils.toColor(instance.getValue(key));
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
        DEFAULT_VALUES.put(HTTP_CLIENT_PROXY_PORT, 80);
        DEFAULT_VALUES.put(HTTP_CLIENT_PROXY_USERNAME, "");
        DEFAULT_VALUES.put(HTTP_CLIENT_PROXY_PASSWORD, "");

        DEFAULT_VALUES.put(EMAIL_SMTP_HOST, "");
        DEFAULT_VALUES.put(EMAIL_SMTP_PORT, 25);
        DEFAULT_VALUES.put(EMAIL_FROM_ADDRESS, "");
        DEFAULT_VALUES.put(EMAIL_SMTP_USERNAME, "");
        DEFAULT_VALUES.put(EMAIL_SMTP_PASSWORD, "");
        DEFAULT_VALUES.put(EMAIL_FROM_NAME, "Mango Automation");
        DEFAULT_VALUES.put(EMAIL_SEND_TIMEOUT, 60000);

        DEFAULT_VALUES.put(POINT_DATA_PURGE_PERIOD_TYPE, Common.TimePeriods.YEARS);
        DEFAULT_VALUES.put(POINT_DATA_PURGE_PERIODS, 1);
        DEFAULT_VALUES.put(POINT_DATA_PURGE_COUNT, true);

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
        DEFAULT_VALUES.put(IMPORTANT_ALARM_PURGE_PERIOD_TYPE, Common.TimePeriods.YEARS);
        DEFAULT_VALUES.put(IMPORTANT_ALARM_PURGE_PERIODS, 1);
        DEFAULT_VALUES.put(WARNING_ALARM_PURGE_PERIOD_TYPE, Common.TimePeriods.YEARS);
        DEFAULT_VALUES.put(WARNING_ALARM_PURGE_PERIODS, 1);
        DEFAULT_VALUES.put(URGENT_ALARM_PURGE_PERIOD_TYPE, Common.TimePeriods.YEARS);
        DEFAULT_VALUES.put(URGENT_ALARM_PURGE_PERIODS, 1);
        DEFAULT_VALUES.put(CRITICAL_ALARM_PURGE_PERIOD_TYPE, Common.TimePeriods.YEARS);
        DEFAULT_VALUES.put(CRITICAL_ALARM_PURGE_PERIODS, 1);
        DEFAULT_VALUES.put(LIFE_SAFETY_ALARM_PURGE_PERIOD_TYPE, Common.TimePeriods.YEARS);
        DEFAULT_VALUES.put(LIFE_SAFETY_ALARM_PURGE_PERIODS, 1);

        DEFAULT_VALUES.put(EVENT_PURGE_PERIOD_TYPE, Common.TimePeriods.YEARS);
        DEFAULT_VALUES.put(EVENT_PURGE_PERIODS, 1);

        DEFAULT_VALUES.put(LANGUAGE, Locale.getDefault().toString());

        DEFAULT_VALUES.put(HTTPDS_PROLOGUE, "");
        DEFAULT_VALUES.put(HTTPDS_EPILOGUE, "");
        DEFAULT_VALUES.put(FUTURE_DATE_LIMIT_PERIODS, 24);
        DEFAULT_VALUES.put(FUTURE_DATE_LIMIT_PERIOD_TYPE, Common.TimePeriods.HOURS);
        DEFAULT_VALUES.put(INSTANCE_DESCRIPTION, "My Mango Automation");

        DEFAULT_VALUES.put(CHART_BACKGROUND_COLOUR, "rgba(255,255,255,0)");
        DEFAULT_VALUES.put(PLOT_BACKGROUND_COLOUR, "rgba(255,255,255,0)");
        DEFAULT_VALUES.put(PLOT_GRIDLINE_COLOUR, "rgba(0,0,0,0.4)");

        //Default Backup Settings
        DEFAULT_VALUES.put(BACKUP_FILE_LOCATION, Common.MA_HOME_PATH.resolve("backup").toString());
        DEFAULT_VALUES.put(BACKUP_PERIOD_TYPE, Common.TimePeriods.DAYS); //Backup Daily
        DEFAULT_VALUES.put(BACKUP_PERIODS, 1);
        DEFAULT_VALUES.put(BACKUP_FILE_COUNT, 10);
        DEFAULT_VALUES.put(BACKUP_HOUR, 0);
        DEFAULT_VALUES.put(BACKUP_MINUTE, 5);
        //Can't use boolean here... DEFAULT_VALUES.put(BACKUP_ENABLED, true);
        //DEFAULT_VALUES.put(ALLOW_ANONYMOUS_GRAPHIC_VIEWS, false);
        DEFAULT_VALUES.put(DATABASE_BACKUP_FILE_LOCATION, Common.MA_HOME_PATH.resolve("backup").toString());
        DEFAULT_VALUES.put(DATABASE_BACKUP_PERIOD_TYPE, Common.TimePeriods.DAYS); //Backup Daily
        DEFAULT_VALUES.put(DATABASE_BACKUP_PERIODS, 1);
        DEFAULT_VALUES.put(DATABASE_BACKUP_FILE_COUNT, 10);
        DEFAULT_VALUES.put(DATABASE_BACKUP_HOUR, 0);
        DEFAULT_VALUES.put(DATABASE_BACKUP_MINUTE, 5);

        DEFAULT_VALUES.put(HIGH_PRI_CORE_POOL_SIZE, 1);
        DEFAULT_VALUES.put(HIGH_PRI_MAX_POOL_SIZE, 100);
        DEFAULT_VALUES.put(MED_PRI_CORE_POOL_SIZE, 3);
        DEFAULT_VALUES.put(LOW_PRI_CORE_POOL_SIZE, 1);

        DEFAULT_VALUES.put(UPGRADE_VERSION_STATE, UpgradeVersionState.PRODUCTION);

        DEFAULT_VALUES.put(DataPurge.ENABLE_POINT_DATA_PURGE, true);
        DEFAULT_VALUES.put(DATABASE_BACKUP_ENABLED, true);
        DEFAULT_VALUES.put(BACKUP_ENABLED, true);

        // Add module system event type defaults
        for (SystemEventTypeDefinition def : ModuleRegistry.getDefinitions(SystemEventTypeDefinition.class))
            DEFAULT_VALUES.put(SystemEventType.SYSTEM_SETTINGS_PREFIX + def.getTypeName(), def.getDefaultAlarmLevel().value());

        // Add built-in audit event type defaults
        DEFAULT_VALUES.put(AuditEventType.AUDIT_SETTINGS_PREFIX + AuditEventType.TYPE_DATA_SOURCE, AlarmLevels.INFORMATION.value());
        DEFAULT_VALUES.put(AuditEventType.AUDIT_SETTINGS_PREFIX + AuditEventType.TYPE_DATA_POINT, AlarmLevels.INFORMATION.value());
        DEFAULT_VALUES.put(AuditEventType.AUDIT_SETTINGS_PREFIX + AuditEventType.TYPE_EVENT_HANDLER, AlarmLevels.INFORMATION.value());
        DEFAULT_VALUES.put(AuditEventType.AUDIT_SETTINGS_PREFIX + AuditEventType.TYPE_TEMPLATE, AlarmLevels.INFORMATION.value());
        DEFAULT_VALUES.put(AuditEventType.AUDIT_SETTINGS_PREFIX + AuditEventType.TYPE_USER_COMMENT, AlarmLevels.INFORMATION.value());
        DEFAULT_VALUES.put(AuditEventType.AUDIT_SETTINGS_PREFIX + AuditEventType.TYPE_USER, AlarmLevels.INFORMATION.value());
        DEFAULT_VALUES.put(AuditEventType.AUDIT_SETTINGS_PREFIX + AuditEventType.TYPE_JSON_DATA, AlarmLevels.INFORMATION.value());
        DEFAULT_VALUES.put(AuditEventType.AUDIT_SETTINGS_PREFIX + AuditEventType.TYPE_EVENT_DETECTOR, AlarmLevels.INFORMATION.value());
        DEFAULT_VALUES.put(AuditEventType.AUDIT_SETTINGS_PREFIX + AuditEventType.TYPE_PUBLISHER, AlarmLevels.INFORMATION.value());

        DEFAULT_VALUES.put(HTTP_SESSION_TIMEOUT_PERIOD_TYPE, Common.TimePeriods.HOURS);
        DEFAULT_VALUES.put(HTTP_SESSION_TIMEOUT_PERIODS, 48);

        DEFAULT_VALUES.put(PASSWORD_EXPIRATION_ENABLED, false);
        DEFAULT_VALUES.put(PASSWORD_EXPIRATION_PERIOD_TYPE, Common.TimePeriods.MONTHS);
        DEFAULT_VALUES.put(PASSWORD_EXPIRATION_PERIODS, 6);

        DEFAULT_VALUES.put(USAGE_TRACKING_ENABLED, false);
        DEFAULT_VALUES.put(UPGRADE_CHECKS_ENABLED, true);
        DEFAULT_VALUES.put(LICENSE_AGREEMENT_VERSION, 0);

        DEFAULT_VALUES.put(RESTART_DELAY, 10);

        //Timeouts for tokens
        DEFAULT_VALUES.put(EmailAddressVerificationService.EXPIRY_SYSTEM_SETTING, EmailAddressVerificationService.DEFAULT_EXPIRY_DURATION);
        DEFAULT_VALUES.put(PasswordResetService.EXPIRY_SYSTEM_SETTING, PasswordResetService.DEFAULT_EXPIRY_DURATION);

        // Add module audit event type defaults
        for (AuditEventTypeDefinition def : ModuleRegistry.getDefinitions(AuditEventTypeDefinition.class)) {
            DEFAULT_VALUES.put(AuditEventType.AUDIT_SETTINGS_PREFIX + def.getTypeName(), AlarmLevels.INFORMATION.value());
        }

        //Module Defaults
        Map<String,Object> modDefaults = null;
        for(SystemSettingsDefinition def : ModuleRegistry.getSystemSettingsDefinitions()){
            modDefaults = def.getDefaultValues();
            if(modDefaults != null)
                DEFAULT_VALUES.putAll(modDefaults);
            modDefaults = null;
        }

        DEFAULT_VALUES.put(PASSWORD_UPPER_CASE_COUNT, 0);
        DEFAULT_VALUES.put(PASSWORD_LOWER_CASE_COUNT, 0);
        DEFAULT_VALUES.put(PASSWORD_DIGIT_COUNT, 0);
        DEFAULT_VALUES.put(PASSWORD_SPECIAL_COUNT, 0);
        DEFAULT_VALUES.put(PASSWORD_LENGTH_MIN, 8);
        DEFAULT_VALUES.put(PASSWORD_LENGTH_MAX, 255);
        DEFAULT_VALUES.put(EMAIL_DISABLED, false);
    }

    /**
     * Save values to the table by replacing old values and inserting new ones
     * caution, there is no checking on quality of the values being saved use
     * validate() first.
     *
     * @param vo
     * @throws JsonProcessingException
     */
    public void updateSettings(Map<String, Object> settings) {
        for (Entry<String, Object> entry : settings.entrySet()) {
            String setting = entry.getKey();
            // Lookup the setting to see if it exists
            Object value = entry.getValue();

            String stringValue;
            if (value instanceof Boolean) {
                stringValue = boolToChar((Boolean) value);
            } else if (value instanceof String) {
                // Can we convert the value to ensure we don't save the String values
                Integer converted = convertToValueFromCode(setting, (String) value);
                if (converted != null)
                    stringValue = Integer.toString(converted);
                else
                    stringValue = (String) value;
            } else if (value instanceof JsonNode) {
                try {
                    stringValue = mapper.writeValueAsString(value);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            } else if (value == null) {
                stringValue = null;
            } else {
                stringValue = value.toString();
            }
            setValue(setting, stringValue);
        }
    }

    /**
     *
     * Validate the system settings passed in, only validating settings that exist in the map.
     *
     * @param settings
     * @param voResponse
     */
    public void validate(Map<String, Object> settings, ProcessResult response, PermissionHolder user) {

        Object setting = null;

        try{
            setting = settings.get(EMAIL_CONTENT_TYPE);
            if(setting != null){

                if(setting instanceof Number){
                    int emailContentType = ((Number)setting).intValue();
                    switch(emailContentType){
                        case MangoEmailContent.CONTENT_TYPE_BOTH:
                        case MangoEmailContent.CONTENT_TYPE_HTML:
                        case MangoEmailContent.CONTENT_TYPE_TEXT:
                            break;
                        default:
                            response.addContextualMessage(EMAIL_CONTENT_TYPE, "validate.invalideValue");
                    }
                }else{
                    //String Code
                    if(MangoEmailContent.CONTENT_TYPE_CODES.getId((String)setting) < 0)
                        response.addContextualMessage(EMAIL_CONTENT_TYPE, "emport.error.invalid", EMAIL_CONTENT_TYPE, setting,
                                MangoEmailContent.CONTENT_TYPE_CODES.getCodeList());
                }
            }
        }catch(NumberFormatException e){
            response.addContextualMessage(EMAIL_CONTENT_TYPE, "validate.illegalValue");
        }

        setting = settings.get(EMAIL_SEND_TIMEOUT);
        if(setting != null) {
            if(setting instanceof Integer) {
                if((Integer)setting < 0)
                    response.addContextualMessage(EMAIL_SEND_TIMEOUT, "validate.cannotBeNegative");
            } else if(setting instanceof Number){
                if(((Number)setting).intValue() < 0)
                    response.addContextualMessage(EMAIL_SEND_TIMEOUT, "validate.cannotBeNegative");
            } else {
                response.addContextualMessage(EMAIL_SEND_TIMEOUT, "validate.invalidValue");
            }
        }

        setting = settings.get(DATABASE_SCHEMA_VERSION);
        if(setting != null)
            response.addContextualMessage(DATABASE_SCHEMA_VERSION, "validate.readOnly");
        setting = settings.get(NEW_INSTANCE);
        if(setting != null)
            response.addContextualMessage(NEW_INSTANCE, "validate.readOnly");

        setting = settings.get(LANGUAGE);
        if(setting != null) {
            if(setting instanceof String) {
                try{
                    Locale newLocale = Common.parseLocale((String)setting);
                    if(newLocale == null)
                        response.addContextualMessage(LANGUAGE, "validate.invalidValue");
                }catch(Throwable t) {
                    response.addContextualMessage(LANGUAGE, "validate.invalidValue");
                }
            }else {
                response.addContextualMessage(LANGUAGE, "validate.invalidValue");
            }
        }

        setting = settings.get(HTTP_CLIENT_PROXY_PORT);
        if(setting != null) {
            if(setting instanceof Integer) {
                if((Integer)setting < 0)
                    response.addContextualMessage(HTTP_CLIENT_PROXY_PORT, "validate.cannotBeNegative");
            } else if(setting instanceof Number){
                if(((Number)setting).intValue() < 0)
                    response.addContextualMessage(HTTP_CLIENT_PROXY_PORT, "validate.cannotBeNegative");
            } else {
                response.addContextualMessage(HTTP_CLIENT_PROXY_PORT, "validate.invalidValue");
            }
        }

        validatePeriodType(POINT_DATA_PURGE_PERIOD_TYPE, settings, response);

        validatePeriodType(DATA_POINT_EVENT_PURGE_PERIOD_TYPE, settings, response);
        validatePeriodType(DATA_SOURCE_EVENT_PURGE_PERIOD_TYPE, settings, response);
        validatePeriodType(SYSTEM_EVENT_PURGE_PERIOD_TYPE, settings, response);
        validatePeriodType(PUBLISHER_EVENT_PURGE_PERIOD_TYPE, settings, response);
        validatePeriodType(AUDIT_EVENT_PURGE_PERIOD_TYPE, settings, response);

        validatePeriodType(NONE_ALARM_PURGE_PERIOD_TYPE, settings, response);
        validatePeriodType(INFORMATION_ALARM_PURGE_PERIOD_TYPE, settings, response);
        validatePeriodType(IMPORTANT_ALARM_PURGE_PERIOD_TYPE, settings, response);
        validatePeriodType(WARNING_ALARM_PURGE_PERIOD_TYPE, settings, response);
        validatePeriodType(URGENT_ALARM_PURGE_PERIOD_TYPE, settings, response);
        validatePeriodType(CRITICAL_ALARM_PURGE_PERIOD_TYPE, settings, response);
        validatePeriodType(LIFE_SAFETY_ALARM_PURGE_PERIOD_TYPE, settings, response);

        validatePeriodType(EVENT_PURGE_PERIOD_TYPE, settings, response);
        validatePeriodType(FUTURE_DATE_LIMIT_PERIOD_TYPE, settings, response);

        try {
            setting = settings.get(FUTURE_DATE_LIMIT_PERIODS);
            if(setting != null) {
                int settingValue;
                if(setting instanceof Number)
                    settingValue = ((Number)setting).intValue();
                else
                    settingValue = Integer.valueOf((String)setting);
                if(settingValue <= 0)
                    response.addContextualMessage(FUTURE_DATE_LIMIT_PERIODS, "validate.greaterThanZero");
            }
        } catch(NumberFormatException|ClassCastException e) {
            response.addContextualMessage(FUTURE_DATE_LIMIT_PERIODS, "validate.invalidValue");
        }

        //Should validate language not sure how yet

        try {
            setting = settings.get(CHART_BACKGROUND_COLOUR);
            if(setting != null)
                ColorUtils.toColor((String)setting);
        }
        catch (InvalidArgumentException e) {
            response.addContextualMessage(CHART_BACKGROUND_COLOUR,
                    "systemSettings.validation.invalidColour");
        }

        try {
            setting = settings.get(PLOT_BACKGROUND_COLOUR);
            if(setting != null)
                ColorUtils.toColor((String)setting);
        }
        catch (InvalidArgumentException e) {
            response.addContextualMessage(PLOT_BACKGROUND_COLOUR,
                    "systemSettings.validation.invalidColour");
        }

        try {
            setting = settings.get(PLOT_GRIDLINE_COLOUR);
            if(setting != null)
                ColorUtils.toColor((String)setting);
        }
        catch (InvalidArgumentException e) {
            response.addContextualMessage(PLOT_GRIDLINE_COLOUR,
                    "systemSettings.validation.invalidColour");
        }

        setting = settings.get(BACKUP_FILE_LOCATION);
        if(setting != null){
            File tmp = new File((String)setting);
            if(!tmp.exists() && !tmp.mkdirs()){
                //Doesn't exist, push up message
                response.addContextualMessage(BACKUP_FILE_LOCATION,
                        "systemSettings.validation.backupLocationNotExists");
            }
            if(!tmp.canWrite()){
                response.addContextualMessage(BACKUP_FILE_LOCATION,
                        "systemSettings.validation.cannotWriteToBackupFileLocation");
            }
        }

        //Validate the Hour and Minute
        Integer backupHour = getIntValue(BACKUP_HOUR, settings);
        if(backupHour != null)
            if((backupHour > 23)||(backupHour<0)){
                response.addContextualMessage(BACKUP_HOUR,
                        "systemSettings.validation.backupHourInvalid");
            }

        Integer backupMinute = getIntValue(BACKUP_MINUTE, settings);
        if(backupMinute != null)
            if((backupMinute > 59)||(backupMinute<0)){
                response.addContextualMessage(BACKUP_MINUTE,
                        "systemSettings.validation.backupMinuteInvalid");
            }

        validatePeriodType(BACKUP_PERIOD_TYPE, settings, response);

        //Validate the number of backups to keep
        Integer backupFileCount = getIntValue(BACKUP_FILE_COUNT, settings);
        if(backupFileCount != null)
            if(backupFileCount < 1){
                response.addContextualMessage(BACKUP_FILE_COUNT,
                        "systemSettings.validation.backupFileCountInvalid");
            }


        //Validate
        setting = settings.get(DATABASE_BACKUP_FILE_LOCATION);
        if(setting != null){
            File tmp = new File((String)setting);
            if(!tmp.exists() && !tmp.mkdirs()){
                //Doesn't exist, push up message
                response.addContextualMessage(DATABASE_BACKUP_FILE_LOCATION,
                        "systemSettings.validation.databaseBackupLocationNotExists");
            }
            if(!tmp.canWrite()){
                response.addContextualMessage(DATABASE_BACKUP_FILE_LOCATION,
                        "systemSettings.validation.cannotWriteToDatabaseBackupFileLocation");
            }
        }

        //Validate the Hour and Minute
        Integer databaseBackupHour = getIntValue(DATABASE_BACKUP_HOUR, settings);
        if(databaseBackupHour != null)
            if((databaseBackupHour > 23)||(databaseBackupHour<0)){
                response.addContextualMessage(DATABASE_BACKUP_HOUR,
                        "systemSettings.validation.databaseBackupHourInvalid");
            }

        Integer databaseBackupMinute = getIntValue(DATABASE_BACKUP_MINUTE, settings);
        if(databaseBackupMinute != null)
            if((databaseBackupMinute > 59)||(databaseBackupMinute<0)){
                response.addContextualMessage(DATABASE_BACKUP_MINUTE,
                        "systemSettings.validation.databaseBackupMinuteInvalid");
            }

        validatePeriodType(DATABASE_BACKUP_PERIOD_TYPE, settings, response);

        //Validate the number of backups to keep
        Integer databaseBackupFileCount = getIntValue(DATABASE_BACKUP_FILE_COUNT, settings);
        if(databaseBackupFileCount != null)
            if(databaseBackupFileCount < 1){
                response.addContextualMessage(DATABASE_BACKUP_FILE_COUNT,
                        "systemSettings.validation.databaseBackupFileCountInvalid");
            }

        //Thread Pool Sizes
        Integer corePoolSize = getIntValue(HIGH_PRI_CORE_POOL_SIZE, settings);
        Integer maxPoolSize = getIntValue(HIGH_PRI_MAX_POOL_SIZE, settings);

        if((corePoolSize != null)&&(corePoolSize < 0)){
            response.addContextualMessage(HIGH_PRI_CORE_POOL_SIZE, "validate.greaterThanOrEqualTo", 0);
        }

        if((maxPoolSize != null)&&(maxPoolSize < BackgroundProcessing.HIGH_PRI_MAX_POOL_SIZE_MIN)){
            response.addContextualMessage(HIGH_PRI_MAX_POOL_SIZE, "validate.greaterThanOrEqualTo", BackgroundProcessing.HIGH_PRI_MAX_POOL_SIZE_MIN);
        }

        if((maxPoolSize != null)&&(maxPoolSize < corePoolSize)){
            response.addContextualMessage(HIGH_PRI_MAX_POOL_SIZE, "systemSettings.threadPools.validate.maxPoolMustBeGreaterThanCorePool");
        }

        //For Medium and Low the Max has no effect because they use a LinkedBlockingQueue and will just block until a
        // core pool thread is available
        corePoolSize = getIntValue(MED_PRI_CORE_POOL_SIZE, settings);
        if(corePoolSize < BackgroundProcessing.MED_PRI_MAX_POOL_SIZE_MIN){
            response.addContextualMessage(MED_PRI_CORE_POOL_SIZE, "validate.greaterThanOrEqualTo", BackgroundProcessing.MED_PRI_MAX_POOL_SIZE_MIN);
        }

        corePoolSize = getIntValue(LOW_PRI_CORE_POOL_SIZE, settings);
        if(corePoolSize < BackgroundProcessing.LOW_PRI_MAX_POOL_SIZE_MIN){
            response.addContextualMessage(LOW_PRI_CORE_POOL_SIZE, "validate.greaterThanOrEqualTo", BackgroundProcessing.LOW_PRI_MAX_POOL_SIZE_MIN);
        }

        //Validate Upgrade Version State
        setting = settings.get(UPGRADE_VERSION_STATE);
        if(setting != null){
            if(setting instanceof Number){
                //Legacy Integer Value
                try{
                    int value = ((Number)setting).intValue();

                    switch(value){
                        case UpgradeVersionState.DEVELOPMENT:
                        case UpgradeVersionState.ALPHA:
                        case UpgradeVersionState.BETA:
                        case UpgradeVersionState.RELEASE_CANDIDATE:
                        case UpgradeVersionState.PRODUCTION:
                            break;
                        default:
                            response.addContextualMessage(UPGRADE_VERSION_STATE, "validate.invalidValue");
                            break;
                    }
                }catch(NumberFormatException e){
                    response.addContextualMessage(UPGRADE_VERSION_STATE, "validate.illegalValue");
                }
            }else{
                //Must be a code
                if(Common.VERSION_STATE_CODES.getId((String)setting) < 0)
                    response.addContextualMessage(UPGRADE_VERSION_STATE, "emport.error.invalid", UPGRADE_VERSION_STATE, setting,
                            Common.VERSION_STATE_CODES.getCodeList());
            }
        }

        // Validate the Module Settings
        for (SystemSettingsDefinition def : ModuleRegistry.getSystemSettingsDefinitions())
            def.validateSettings(settings, response);

        //Ensure no one can change the superadmin permission
        setting = settings.get(SuperadminPermissionDefinition.PERMISSION);
        if(setting != null) {
            try {
                if(!((String)setting).equals(SuperadminPermissionDefinition.GROUP_NAME))
                    response.addContextualMessage(SuperadminPermissionDefinition.PERMISSION, "validate.readOnly");
            } catch(ClassCastException e) {
                response.addContextualMessage(SuperadminPermissionDefinition.PERMISSION, "validate.readOnly");
            }
        }

        //Validate system alarm levels
        for (SystemEventTypeDefinition def : ModuleRegistry.getDefinitions(SystemEventTypeDefinition.class))
            validateAlarmLevel(SystemEventType.SYSTEM_SETTINGS_PREFIX + def.getTypeName(), settings, response);

        //Validate audit alarm levels
        validateAlarmLevel(AuditEventType.AUDIT_SETTINGS_PREFIX + AuditEventType.TYPE_DATA_SOURCE, settings, response);
        validateAlarmLevel(AuditEventType.AUDIT_SETTINGS_PREFIX + AuditEventType.TYPE_DATA_POINT, settings, response);
        validateAlarmLevel(AuditEventType.AUDIT_SETTINGS_PREFIX + AuditEventType.TYPE_EVENT_HANDLER, settings, response);
        validateAlarmLevel(AuditEventType.AUDIT_SETTINGS_PREFIX + AuditEventType.TYPE_TEMPLATE, settings, response);
        validateAlarmLevel(AuditEventType.AUDIT_SETTINGS_PREFIX + AuditEventType.TYPE_USER_COMMENT, settings, response);
        validateAlarmLevel(AuditEventType.AUDIT_SETTINGS_PREFIX + AuditEventType.TYPE_USER, settings, response);
        validateAlarmLevel(AuditEventType.AUDIT_SETTINGS_PREFIX + AuditEventType.TYPE_JSON_DATA, settings, response);
        validateAlarmLevel(AuditEventType.AUDIT_SETTINGS_PREFIX + AuditEventType.TYPE_EVENT_DETECTOR, settings, response);
        validateAlarmLevel(AuditEventType.AUDIT_SETTINGS_PREFIX + AuditEventType.TYPE_PUBLISHER, settings, response);
        for (AuditEventTypeDefinition def : ModuleRegistry.getDefinitions(AuditEventTypeDefinition.class))
            validateAlarmLevel(AuditEventType.AUDIT_SETTINGS_PREFIX + def.getTypeName(), settings, response);

        validatePeriodType(HTTP_SESSION_TIMEOUT_PERIOD_TYPE, settings, response, Common.TimePeriods.MILLISECONDS);
        Integer timeoutPeriods = getIntValue(HTTP_SESSION_TIMEOUT_PERIODS, settings);
        if(timeoutPeriods != null) {
            if(timeoutPeriods < 1)
                response.addContextualMessage(HTTP_SESSION_TIMEOUT_PERIODS, "validate.invalidValue");
        }

        validatePeriodType(PASSWORD_EXPIRATION_PERIOD_TYPE, settings, response, Common.TimePeriods.MILLISECONDS, Common.TimePeriods.SECONDS);
        Integer passwordExpirationPeriods = getIntValue(PASSWORD_EXPIRATION_PERIODS, settings);
        if(passwordExpirationPeriods != null && passwordExpirationPeriods < 1)
            response.addContextualMessage(PASSWORD_EXPIRATION_PERIODS, "validate.greaterThanZero");

        setting = settings.get(LICENSE_AGREEMENT_VERSION);
        if(setting != null)
            response.addContextualMessage(LICENSE_AGREEMENT_VERSION, "validate.readOnly");

        setting = settings.get(PUBLICLY_RESOLVABLE_BASE_URL);
        if (setting != null) {
            try {
                UriComponentsBuilder.fromHttpUrl((String) setting).build().toUri();
            } catch (Exception e) {
                response.addContextualMessage(PUBLICLY_RESOLVABLE_BASE_URL, "validate.invalidValue");
            }
        }

        //Validate password settings
        Integer passwordLengthMin = getIntValue(PASSWORD_LENGTH_MIN, settings);
        Integer passwordLengthMax = getIntValue(PASSWORD_LENGTH_MAX, settings);
        if(passwordLengthMin > 255) {
            response.addContextualMessage(PASSWORD_LENGTH_MIN, "validate.lessThanOrEqualTo", 255);
        }
        if(passwordLengthMin < 1) {
            response.addContextualMessage(PASSWORD_LENGTH_MIN, "validate.greaterThanOrEqualTo", 1);
        }
        if(passwordLengthMax > 255) {
            response.addContextualMessage(PASSWORD_LENGTH_MAX, "validate.lessThanOrEqualTo", 255);
        }
        if(passwordLengthMax < 1) {
            response.addContextualMessage(PASSWORD_LENGTH_MAX, "validate.greaterThanOrEqualTo", 1);
        }
        if(passwordLengthMin > passwordLengthMax) {
            response.addContextualMessage(PASSWORD_LENGTH_MAX, "validate.greaterThanOrEqualTo", passwordLengthMin);
            response.addContextualMessage(PASSWORD_LENGTH_MIN, "validate.lessThanOrEqualTo", passwordLengthMax);
        }

        Integer passwordSetting = getIntValue(PASSWORD_UPPER_CASE_COUNT, settings);
        if(passwordSetting != 0) {
            if (passwordSetting < 0) {
                response.addContextualMessage(PASSWORD_UPPER_CASE_COUNT, "validate.greaterThanOrEqualTo", 0);
            }else if(passwordSetting > passwordLengthMax) {
                response.addContextualMessage(PASSWORD_UPPER_CASE_COUNT, "validate.lessThanOrEqualTo", passwordLengthMax);
            }
        }
        passwordSetting = getIntValue(PASSWORD_LOWER_CASE_COUNT, settings);
        if(passwordSetting != 0) {
            if (passwordSetting < 0) {
                response.addContextualMessage(PASSWORD_LOWER_CASE_COUNT, "validate.greaterThanOrEqualTo", 0);
            }else if(passwordSetting > passwordLengthMax) {
                response.addContextualMessage(PASSWORD_LOWER_CASE_COUNT, "validate.lessThanOrEqualTo", passwordLengthMax);
            }
        }
        passwordSetting = getIntValue(PASSWORD_DIGIT_COUNT, settings);
        if(passwordSetting != 0) {
            if (passwordSetting < 0) {
                response.addContextualMessage(PASSWORD_DIGIT_COUNT, "validate.greaterThanOrEqualTo", 0);
            }else if(passwordSetting > passwordLengthMax) {
                response.addContextualMessage(PASSWORD_DIGIT_COUNT, "validate.lessThanOrEqualTo", passwordLengthMax);
            }
        }
        passwordSetting = getIntValue(PASSWORD_SPECIAL_COUNT, settings);
        if(passwordSetting != 0) {
            if (passwordSetting < 0) {
                response.addContextualMessage(PASSWORD_SPECIAL_COUNT, "validate.greaterThanOrEqualTo", 0);
            }else if(passwordSetting > passwordLengthMax) {
                response.addContextualMessage(PASSWORD_SPECIAL_COUNT, "validate.lessThanOrEqualTo", passwordLengthMax);
            }
        }
    }

    /**
     * @param pointDataPurgePeriodType2
     * @param pointDataPurgePeriodType3
     * @param response
     */
    private void validatePeriodType(String key, Map<String,Object> settings, ProcessResult response) {
        validatePeriodType(key, settings, response, null);
    }

    private void validatePeriodType(String key, Map<String,Object> settings, ProcessResult response, int...excludeIds) {
        Object setting = settings.get(key);
        if(setting == null)
            return;

        if(setting instanceof Number){
            try{
                int value = ((Number)setting).intValue();

                if(ArrayUtils.contains(excludeIds, value)) {
                    response.addContextualMessage(key, "validate.invalidValue");
                    return;
                }

                switch(value){
                    case TimePeriods.DAYS:
                    case TimePeriods.HOURS:
                    case TimePeriods.MILLISECONDS:
                    case TimePeriods.MINUTES:
                    case TimePeriods.MONTHS:
                    case TimePeriods.SECONDS:
                    case TimePeriods.WEEKS:
                    case TimePeriods.YEARS:
                        break;
                    default:
                        response.addContextualMessage(key, "validate.invalidValue");
                }
            }catch(NumberFormatException e){
                response.addContextualMessage(key, "validate.illegalValue");
            }
        }else{
            //String code
            if(Common.TIME_PERIOD_CODES.getId((String)setting, excludeIds) < 0)
                response.addContextualMessage( key, "emport.error.invalid", key, setting,
                        Common.TIME_PERIOD_CODES.getCodeList());
        }
    }

    private void validateAlarmLevel(String key, Map<String, Object> settings, ProcessResult response) {
        Object setting = settings.get(key);
        if(setting == null)
            return;

        if (setting instanceof Number) {
            try {
                AlarmLevels.fromValue(((Number) setting).intValue());
            } catch (IllegalArgumentException | NullPointerException e) {
                response.addContextualMessage(key, "validate.illegalValue");
            }
        } else {
            try {
                AlarmLevels.fromName((String) setting);
            } catch (IllegalArgumentException | NullPointerException e) {
                response.addContextualMessage(key, "emport.error.invalid", key, setting, Arrays.asList(AlarmLevels.values()));
            }
        }
    }

    private Integer getIntValue(String key, Map<String,Object> settings) throws NumberFormatException {
        Object value = settings.get(key);
        if (value == null)
            value = getIntValue(key);

        if(value instanceof Number)
            return ((Number) value).intValue();
        else
            return Integer.parseInt((String)value);
    }

    /**
     * Potentially Convert a value from a code, if no code exists then return null;
     *
     * @param key - Setting key
     * @param value - String code
     * @return
     */
    public Integer convertToValueFromCode(String key, String code) {
        switch(key){
            case EMAIL_CONTENT_TYPE:
                return MangoEmailContent.CONTENT_TYPE_CODES.getId(code);
            case POINT_DATA_PURGE_PERIOD_TYPE:
            case DATA_POINT_EVENT_PURGE_PERIOD_TYPE:
            case DATA_SOURCE_EVENT_PURGE_PERIOD_TYPE:
            case SYSTEM_EVENT_PURGE_PERIOD_TYPE:
            case PUBLISHER_EVENT_PURGE_PERIOD_TYPE:
            case AUDIT_EVENT_PURGE_PERIOD_TYPE:
            case NONE_ALARM_PURGE_PERIOD_TYPE:
            case INFORMATION_ALARM_PURGE_PERIOD_TYPE:
            case IMPORTANT_ALARM_PURGE_PERIOD_TYPE:
            case WARNING_ALARM_PURGE_PERIOD_TYPE:
            case URGENT_ALARM_PURGE_PERIOD_TYPE:
            case CRITICAL_ALARM_PURGE_PERIOD_TYPE:
            case LIFE_SAFETY_ALARM_PURGE_PERIOD_TYPE:
            case EVENT_PURGE_PERIOD_TYPE:
            case BACKUP_PERIOD_TYPE:
            case DATABASE_BACKUP_PERIOD_TYPE:
            case FUTURE_DATE_LIMIT_PERIOD_TYPE:
            case HTTP_SESSION_TIMEOUT_PERIOD_TYPE:
            case PASSWORD_EXPIRATION_PERIOD_TYPE:
                return Common.TIME_PERIOD_CODES.getId(code);
            case UPGRADE_VERSION_STATE:
                return Common.VERSION_STATE_CODES.getId(code);
        }

        //Is it an alarm level?
        if(key != null && (key.startsWith(SystemEventType.SYSTEM_SETTINGS_PREFIX) || key.startsWith(AuditEventType.AUDIT_SETTINGS_PREFIX)))
            return AlarmLevels.fromName(code).value();

        //Now try the SystemSettingsDefinitions
        Integer value = null;
        for(SystemSettingsDefinition def : ModuleRegistry.getSystemSettingsDefinitions()){
            value = def.convertToValueFromCode(key, code);
            if(value != null)
                return value;
        }

        return null;
    }

    /**
     * Potentially convert an Integer value from it's Export code value to its export code
     * @param key - Setting key
     * @param value - Integer value of code
     * @return Export code or null if none exists for it
     */
    public String convertToCodeFromValue(String key, Integer value){
        switch(key){
            case EMAIL_CONTENT_TYPE:
                return MangoEmailContent.CONTENT_TYPE_CODES.getCode(value);
            case POINT_DATA_PURGE_PERIOD_TYPE:
            case DATA_POINT_EVENT_PURGE_PERIOD_TYPE:
            case DATA_SOURCE_EVENT_PURGE_PERIOD_TYPE:
            case SYSTEM_EVENT_PURGE_PERIOD_TYPE:
            case PUBLISHER_EVENT_PURGE_PERIOD_TYPE:
            case AUDIT_EVENT_PURGE_PERIOD_TYPE:
            case NONE_ALARM_PURGE_PERIOD_TYPE:
            case INFORMATION_ALARM_PURGE_PERIOD_TYPE:
            case IMPORTANT_ALARM_PURGE_PERIOD_TYPE:
            case WARNING_ALARM_PURGE_PERIOD_TYPE:
            case URGENT_ALARM_PURGE_PERIOD_TYPE:
            case CRITICAL_ALARM_PURGE_PERIOD_TYPE:
            case LIFE_SAFETY_ALARM_PURGE_PERIOD_TYPE:
            case EVENT_PURGE_PERIOD_TYPE:
            case BACKUP_PERIOD_TYPE:
            case DATABASE_BACKUP_PERIOD_TYPE:
            case FUTURE_DATE_LIMIT_PERIOD_TYPE:
            case HTTP_SESSION_TIMEOUT_PERIOD_TYPE:
            case PASSWORD_EXPIRATION_PERIOD_TYPE:
                return Common.TIME_PERIOD_CODES.getCode(value);
            case UPGRADE_VERSION_STATE:
                return Common.VERSION_STATE_CODES.getCode(value);
        }

        //Check if it's an alarm level setting...
        if(key != null && (key.startsWith(SystemEventType.SYSTEM_SETTINGS_PREFIX) || key.startsWith(AuditEventType.AUDIT_SETTINGS_PREFIX)))
            return AlarmLevels.fromValue(value).name();

        //Now try the SystemSettingsDefinitions
        String code = null;
        for(SystemSettingsDefinition def : ModuleRegistry.getSystemSettingsDefinitions()){
            code = def.convertToCodeFromValue(key, value);
            if(code != null)
                return code;
        }
        return null;
    }

    /**
     * Make a copy of the map and convert any Export codes into their values
     * @param settings
     * @return
     */
    public Map<String, Object> convertCodesToValues(Map<String, Object> settings) {
        Map<String, Object> values = new HashMap<String,Object>(settings);

        values.replaceAll((key, value) -> {
            if (value instanceof String) {
                Integer code = convertToValueFromCode(key, (String) value);
                if (code != null) return code;
            }
            return value;
        });

        return values;
    }

    /**
     * Return all settings (if no setting is saved return default value) whilst converting to Export Codes where necessary
     *
     * @return
     */
    public Map<String, Object> getAllSystemSettingsAsCodes() {
        Map<String, Object> settings = new HashMap<String,Object>(DEFAULT_VALUES.size());

        //Start with all the defaults
        Iterator<String> it = DEFAULT_VALUES.keySet().iterator();
        String key;
        while(it.hasNext()){
            key = it.next();
            if(!key.toLowerCase().contains("password")&&!key.startsWith(DATABASE_SCHEMA_VERSION))
                settings.put(key, DEFAULT_VALUES.get(key));
        }

        // Then replace anything with what is stored in the database
        ejt.query("select settingName,settingValue from systemSettings", new RowCallbackHandler() {

            @Override
            public void processRow(ResultSet rs) throws SQLException {
                String settingName = rs.getString(1);
                // Don't export any passwords or schema numbers
                if ((!settingName.toLowerCase().contains("password")
                        && !settingName.startsWith(DATABASE_SCHEMA_VERSION))) {
                    String settingValue = rs.getString(2);
                    if (settingValue != null) {
                        // Convert Numbers to Integers
                        try {
                            settings.put(settingName, Integer.parseInt(settingValue));
                        } catch (NumberFormatException e) {
                            // Are we a boolean
                            if (settingValue.equalsIgnoreCase("y")) {
                                settings.put(settingName, new Boolean(true));
                            } else if (settingValue.equalsIgnoreCase("n")) {
                                settings.put(settingName, new Boolean(false));
                            } else {
                                // Must be a string
                                settings.put(settingName, settingValue);
                            }
                        }
                    }else {
                        //If there is no default then set it to the null that was returned in the query
                        //  this preserves the ability to save null settings values
                        //  and also so that there is an indication that a null setting exists
                        if(settings.get(settingName) == null)
                            settings.put(settingName, null);
                    }
                }
            }
        });

        // Convert the Integers to Codes
        it = settings.keySet().iterator();
        while (it.hasNext()) {
            key = it.next();
            Object value = settings.get(key);
            if (value instanceof Integer) {
                String code = convertToCodeFromValue(key, (Integer) value);
                if (code != null)
                    settings.put(key, code);
            }
        }

        return settings;
    }
}
