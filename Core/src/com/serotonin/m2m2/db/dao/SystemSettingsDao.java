/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.db.dao;

import java.awt.Color;
import java.io.File;
import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import com.serotonin.InvalidArgumentException;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.spring.ExtendedJdbcTemplate;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.JsonWriter;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.Common.TimePeriods;
import com.serotonin.m2m2.UpgradeVersionState;
import com.serotonin.m2m2.email.MangoEmailContent;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.module.SystemSettingsDefinition;
import com.serotonin.m2m2.rt.maint.BackgroundProcessing;
import com.serotonin.m2m2.rt.maint.DataPurge;
import com.serotonin.m2m2.util.ColorUtils;
import com.serotonin.m2m2.vo.systemSettings.SystemSettingsEventDispatcher;

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
    public static final String HIGH_PRI_CORE_POOL_SIZE = "highPriorityThreadCorePoolSize";
    public static final String HIGH_PRI_MAX_POOL_SIZE = "highPriorityThreadMaximumPoolSize";
    
    public static final String MED_PRI_CORE_POOL_SIZE = "mediumPriorityThreadCorePoolSize";
    public static final String MED_PRI_MAX_POOL_SIZE = "mediumPriorityThreadMaximumPoolSize";
    
    public static final String LOW_PRI_CORE_POOL_SIZE = "lowPriorityThreadCorePoolSize";
    public static final String LOW_PRI_MAX_POOL_SIZE = "lowPriorityThreadMaximumPoolSize";
    
    //Site analytics
    public static final String SITE_ANALYTICS_HEAD = "siteAnalyticsHead";
    public static final String SITE_ANALYTICS_BODY = "siteAnalyticsBody";
    
    //Download update settings
    public static final String UPGRADE_VERSION_STATE = "upgradeVersionState";
    
    public static SystemSettingsDao instance = new SystemSettingsDao();

    private SystemSettingsDao(){
    	
    }
    
    // Value cache
    private static final Map<String, String> cache = new ConcurrentHashMap<>();

    public static String getValue(String key) {
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

    public static String getValue(String key, String defaultValue) {
        String result = cache.get(key);
        if (result == null) {
            if (!cache.containsKey(key)) {
                result = new BaseDao().queryForObject("select settingValue from systemSettings where settingName=?",
                        new Object[] { key }, String.class, null);
                if (result == null)
                    result = defaultValue;
                else
                	cache.put(key, result); //Only store non-null defaults
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
        Boolean defaultValue = (Boolean) DEFAULT_VALUES.get(key);
        if (defaultValue == null)
            return getBooleanValue(key, false);
        return getBooleanValue(key, defaultValue);
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

        DEFAULT_VALUES.put(FILEDATA_PATH, "~/WEB-INF/filedata");
        DEFAULT_VALUES.put(HTTPDS_PROLOGUE, "");
        DEFAULT_VALUES.put(HTTPDS_EPILOGUE, "");
        DEFAULT_VALUES.put(UI_PERFORMANCE, 2000);
        DEFAULT_VALUES.put(FUTURE_DATE_LIMIT_PERIODS, 24);
        DEFAULT_VALUES.put(FUTURE_DATE_LIMIT_PERIOD_TYPE, Common.TimePeriods.HOURS);
        DEFAULT_VALUES.put(INSTANCE_DESCRIPTION, "My Mango Automation");

        DEFAULT_VALUES.put(CHART_BACKGROUND_COLOUR, "rgba(255,255,255,0)");
        DEFAULT_VALUES.put(PLOT_BACKGROUND_COLOUR, "rgba(255,255,255,0)");
        DEFAULT_VALUES.put(PLOT_GRIDLINE_COLOUR, "rgba(0,0,0,0.4)");

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
        
        DEFAULT_VALUES.put(HIGH_PRI_CORE_POOL_SIZE, 1);   
        DEFAULT_VALUES.put(HIGH_PRI_MAX_POOL_SIZE, 100);   
        DEFAULT_VALUES.put(MED_PRI_CORE_POOL_SIZE, 3);   
        DEFAULT_VALUES.put(MED_PRI_MAX_POOL_SIZE, 30);   
        DEFAULT_VALUES.put(LOW_PRI_CORE_POOL_SIZE, 1);   
        DEFAULT_VALUES.put(LOW_PRI_MAX_POOL_SIZE, 3);
        
        DEFAULT_VALUES.put(UPGRADE_VERSION_STATE, UpgradeVersionState.PRODUCTION);
        
        DEFAULT_VALUES.put(DataPurge.ENABLE_POINT_DATA_PURGE, true);

        //Module Defaults
        Map<String,Object> modDefaults = null;
        for(SystemSettingsDefinition def : ModuleRegistry.getSystemSettingsDefinitions()){
        	modDefaults = def.getDefaultValues();
        	if(modDefaults != null)
        		DEFAULT_VALUES.putAll(modDefaults);
        	modDefaults = null;
        }
    }

	/**
     * Save values to the table by replacing old values and inserting new ones
     * caution, there is no checking on quality of the values being saved use
     * validate() first.
     * 
     * @param vo
     */
    public void updateSettings(Map<String,Object> settings) {

    	Iterator<String> it = settings.keySet().iterator();
    	while(it.hasNext()){
    		String setting = it.next();
    		//Lookup the setting to see if it exists
    		Object value = settings.get(setting);
    		
    		String stringValue;
    		if(value instanceof Boolean){
    			if((Boolean)value)
    				stringValue = "Y";
    			else
    				stringValue = "N";
    		}else if(value instanceof String){
    			//Can we convert the value to ensure we don't save the String values
    			Integer converted = convertToValueFromCode(setting, (String)value);
    			if(converted != null)
    				stringValue = converted.toString();
    			else
    				stringValue = value.toString();
    		}else{
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
	public void validate(Map<String, Object> settings, ProcessResult response) {
		
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
						response.addContextualMessage(EMAIL_CONTENT_TYPE, "emport.error.invalid", EMAIL_CONTENT_TYPE, (String)setting,
								MangoEmailContent.CONTENT_TYPE_CODES.getCodeList());
				}
			}
		}catch(NumberFormatException e){
			response.addContextualMessage(EMAIL_CONTENT_TYPE, "validate.illegalValue");
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
	    	if(!tmp.exists()){
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
	    	if(!tmp.exists()){
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
        maxPoolSize = getIntValue(MED_PRI_MAX_POOL_SIZE, settings);
        if(maxPoolSize < corePoolSize){
        	response.addContextualMessage(MED_PRI_MAX_POOL_SIZE, "systemSettings.threadPools.validate.maxPoolMustBeGreaterThanCorePool");
        }
        if(corePoolSize < BackgroundProcessing.MED_PRI_MAX_POOL_SIZE_MIN){
        	response.addContextualMessage(MED_PRI_CORE_POOL_SIZE, "validate.greaterThanOrEqualTo", BackgroundProcessing.MED_PRI_MAX_POOL_SIZE_MIN);
        }
        
        corePoolSize = getIntValue(LOW_PRI_CORE_POOL_SIZE, settings);
        maxPoolSize = getIntValue(LOW_PRI_MAX_POOL_SIZE, settings);
        if(maxPoolSize < corePoolSize){
        	response.addContextualMessage(LOW_PRI_MAX_POOL_SIZE, "systemSettings.threadPools.validate.maxPoolMustBeGreaterThanCorePool");
        }
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
					response.addContextualMessage(UPGRADE_VERSION_STATE, "emport.error.invalid", UPGRADE_VERSION_STATE, (String)setting,
							Common.VERSION_STATE_CODES.getCodeList());
			}
		}
        
        //Validate the Module Settings
        for(SystemSettingsDefinition def : ModuleRegistry.getSystemSettingsDefinitions())
        	def.validateSettings(settings, response);
	}
	

	/**
	 * @param pointDataPurgePeriodType2
	 * @param pointDataPurgePeriodType3
	 * @param response
	 */
	private void validatePeriodType(String key, Map<String,Object> settings, ProcessResult response) {

		Object setting = settings.get(key);
		if(setting == null)
			return;
			
		if(setting instanceof Number){
			try{
				int value = ((Number)setting).intValue();
				
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
			if(Common.TIME_PERIOD_CODES.getId((String)setting) < 0)
				response.addContextualMessage( key, "emport.error.invalid", key, (String)setting,
						Common.TIME_PERIOD_CODES.getCodeList());
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
			return Common.TIME_PERIOD_CODES.getId(code); 
		case UPGRADE_VERSION_STATE:
			return Common.VERSION_STATE_CODES.getId(code);
		}
		
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
	public static String convertToCodeFromValue(String key, Integer value){
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
			return Common.TIME_PERIOD_CODES.getCode(value);
		case UPGRADE_VERSION_STATE:
			return Common.VERSION_STATE_CODES.getCode(value);
		}
		
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
	public Map<String, Object> convertCodesToValues(Map<String, Object> settings){
		Map<String, Object> values = new HashMap<String,Object>(settings.size());

		Iterator<String> it = settings.keySet().iterator();
		String key;
		while(it.hasNext()){
			key = it.next();
    		Object value = settings.get(key);
    		if(value instanceof String){
	    		Integer code = convertToValueFromCode(key, (String)value);
	    		if(code != null)
	    			values.put(key, code);
    		}else{
    			values.put(key, value);
    		}
		}
		return values;
	}
	
	/**
	 * Return all settings (if no setting is saved return default value) whilst converting to Export Codes where necessary
	 * 
	 * @return
	 */
	public Map<String, Object> getAllSystemSettingsAsCodes() {
		Map<String, Object> settings = new HashMap<String,Object>(DEFAULT_VALUES.size());
		
		//Add The Permissions with empty values since they don't necessarily have defaults
        for (PermissionDefinition def : ModuleRegistry.getDefinitions(PermissionDefinition.class)) {
        	settings.put(def.getPermissionTypeName(), "");
        }
		
		//Start with all the defaults
		Iterator<String> it = DEFAULT_VALUES.keySet().iterator();
		String key;
		while(it.hasNext()){
			key = it.next();
			if(!key.toLowerCase().contains("password")&&!key.startsWith(DATABASE_SCHEMA_VERSION))
				settings.put(key, DEFAULT_VALUES.get(key));
		}
		
		//Then replace anything with what is stored in the database
		ejt.query("select settingName,settingValue from systemSettings", new RowCallbackHandler() {
            
            public void processRow(ResultSet rs) throws SQLException {
            	String settingName = rs.getString(1);

            	//Don't export any passwords or schema numbers
            	if((!settingName.toLowerCase().contains("password")&&!settingName.startsWith(DATABASE_SCHEMA_VERSION))){
            		String settingValue = rs.getString(2);
                	//Convert Numbers to Integers
                	try{
                		settings.put(settingName, Integer.parseInt(settingValue));
                	}catch(NumberFormatException e){
                		//Are we a boolean
                		if(settingValue.equalsIgnoreCase("y")){
                			settings.put(settingName, new Boolean(true));
                		}else if(settingValue.equalsIgnoreCase("n")){
                			settings.put(settingName, new Boolean(false));
                		}else{
                			//Must be a string
                			settings.put(settingName, settingValue);
                		}
                		
                	}
            	}
            }
    	});
		
		//Convert the Integers to Codes
		it = settings.keySet().iterator();
		while(it.hasNext()){
			key = it.next();
    		Object value = settings.get(key);
    		if(value instanceof Integer){
	    		String code = convertToCodeFromValue(key, (Integer)value);
	    		if(code != null)
	    			settings.put(key, code);
    		}
		}
		
		return settings;
	}
	
}
