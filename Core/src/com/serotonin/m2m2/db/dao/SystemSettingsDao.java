/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.db.dao;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import com.serotonin.InvalidArgumentException;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.spring.ExtendedJdbcTemplate;
import com.serotonin.m2m2.Common;
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

    // Event purging
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

    //Backup Settings
	public static final String BACKUP_FILE_LOCATION = "backupFileLocation";
	public static final String BACKUP_PERIOD_TYPE = "backupPeriodType";
	public static final String BACKUP_PERIODS = "backupPeriods";
	public static final String BACKUP_LAST_RUN_SUCCESS = "backupLastSuccessfulRun";
	public static final String BACKUP_HOUR = "backupHour";
	public static final String BACKUP_MINUTE = "backupMinute";
	public static final String BACKUP_FILE_COUNT = "backupFileCount";
	public static final String BACKUP_ENABLED = "backupEnabled";

    // Value cache
    private static final Map<String, String> cache = new HashMap<String, String>();

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

    public void setValue(final String key, final String value) {
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
    }

    public void setIntValue(String key, int value) {
        setValue(key, Integer.toString(value));
    }

    public void setBooleanValue(String key, boolean value) {
        setValue(key, boolToChar(value));
    }

    public void removeValue(String key) {
        // Remove the value from the cache
        cache.remove(key);

        // Reset the cached values too.
        FUTURE_DATE_LIMIT = -1;

        ejt.update("delete from systemSettings where settingName=?", new Object[] { key });
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

    private static final Map<String, Object> DEFAULT_VALUES = new HashMap<String, Object>();
	
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
        DEFAULT_VALUES.put(BACKUP_PERIODS,1);
        DEFAULT_VALUES.put(BACKUP_FILE_COUNT,10);
        DEFAULT_VALUES.put(BACKUP_HOUR, 0);
        DEFAULT_VALUES.put(BACKUP_MINUTE, 5);
        //Can't use boolean here... DEFAULT_VALUES.put(BACKUP_ENABLED, true);
    }

	/**
	 * Get a VO that represents the cached values
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
		vo.setBackupLastRunSuccess(getBooleanValue(BACKUP_LAST_RUN_SUCCESS));
		vo.setBackupFileCount(getIntValue(BACKUP_FILE_COUNT));
		vo.setBackupHour(getIntValue(BACKUP_HOUR));
		vo.setBackupMinute(getIntValue(BACKUP_MINUTE));
		vo.setBackupEnabled(getBooleanValue(BACKUP_ENABLED));
		
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
		this.setIntValue(EMAIL_SMTP_PORT,vo.getEmailSmtpPort());
		this.setValue(EMAIL_FROM_ADDRESS, vo.getEmailFromAddress());
		this.setValue(EMAIL_SMTP_USERNAME, vo.getEmailSmtpUsername());
		this.setValue(EMAIL_SMTP_PASSWORD, vo.getEmailSmtpPassword());
		this.setValue(EMAIL_FROM_NAME, vo.getEmailFromName());
		this.setBooleanValue(EMAIL_AUTHORIZATION, vo.getEmailAuthorization());
		this.setBooleanValue(EMAIL_TLS, vo.getEmailTls());
		this.setIntValue(EMAIL_CONTENT_TYPE,vo.getEmailContentType());
		
		this.setIntValue(POINT_DATA_PURGE_PERIOD_TYPE, vo.getPointDataPurgePeriodType());
		this.setIntValue(POINT_DATA_PURGE_PERIODS,vo.getPointDataPurgePeriods());
		
		this.setIntValue(EVENT_PURGE_PERIOD_TYPE, vo.getEventPurgePeriodType());
		this.setIntValue(EVENT_PURGE_PERIODS, vo.getEventPurgePeriods());
		
		this.setBooleanValue(HTTP_CLIENT_USE_PROXY,vo.getHttpClientUseProxy());
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
		this.setValue(PLOT_BACKGROUND_COLOUR,vo.getChartBackgroundColor());
		this.setValue(PLOT_GRIDLINE_COLOUR, vo.getPlotGridlineColor());
		
		this.setValue(BACKUP_FILE_LOCATION, vo.getBackupFileLocation());
		this.setIntValue(BACKUP_PERIOD_TYPE, vo.getBackupPeriodType());
		this.setIntValue(BACKUP_PERIODS, vo.getBackupPeriods());
		this.setBooleanValue(BACKUP_LAST_RUN_SUCCESS, vo.getBackupLastRunSuccess());
		this.setIntValue(BACKUP_FILE_COUNT, vo.getBackupFileCount());
		this.setIntValue(BACKUP_HOUR, vo.getBackupHour());
		this.setIntValue(BACKUP_MINUTE, vo.getBackupMinute());
		this.setBooleanValue(BACKUP_ENABLED, vo.getBackupEnabled());
	}
    
    
    
    
}
