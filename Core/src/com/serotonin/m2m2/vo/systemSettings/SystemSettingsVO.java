/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.vo.systemSettings;

import java.io.File;
import java.io.IOException;

import com.serotonin.InvalidArgumentException;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.json.spi.JsonSerializable;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.Common.TimePeriods;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.email.MangoEmailContent;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.util.ColorUtils;

/**
 * @author Terry Packer
 *
 */
public class SystemSettingsVO implements JsonSerializable{
	
	@JsonProperty
	private String databaseSchemaVersion;
	@JsonProperty
	private Boolean newInstance;
	
	@JsonProperty
	private String emailSmtpHost;
	@JsonProperty
	private Integer emailSmtpPort;
	@JsonProperty
	private String emailFromAddress;
	@JsonProperty
	private String emailSmtpUsername;
	@JsonProperty
	private String emailSmtpPassword;
	@JsonProperty
	private String emailFromName;
	@JsonProperty
	private Boolean emailAuthorization;
	@JsonProperty
	private Boolean emailTls;
	@JsonProperty
	private Integer emailContentType;
	
	@JsonProperty
	private Integer pointDataPurgePeriodType;
	@JsonProperty
	private Integer pointDataPurgePeriods;
	
	@JsonProperty
	private Integer eventPurgePeriodType;
	@JsonProperty
	private Integer eventPurgePeriods;

	@JsonProperty
	private Boolean httpClientUseProxy;
	@JsonProperty
	private String httpClientProxyServer;
	@JsonProperty
	private Integer httpClientProxyPort;
	@JsonProperty
	private String httpClientProxyUsername;
	@JsonProperty
	private String httpClientProxyPassword;

	@JsonProperty
	private String language;
	
	@JsonProperty
	private String filedataPath;
	@JsonProperty
	private String datasourceDisplaySuffix;
	@JsonProperty
	private String httpdsPrologue;
	@JsonProperty
	private String httpdsEpilogue;
	@JsonProperty
	private Integer uiPerformance;
	@JsonProperty
	private Integer futureDateLimitPeriods;
	@JsonProperty
	private Integer futureDateLimitPeriodType;
	@JsonProperty
	private String instanceDescription;
	
	@JsonProperty
	private String chartBackgroundColor;
	@JsonProperty
	private String plotBackgroundColor;
	@JsonProperty
	private String plotGridlineColor;
	
	@JsonProperty
	private String backupFileLocation;
	@JsonProperty
	private Integer backupPeriodType;
	@JsonProperty
	private Integer backupPeriods;
	@JsonProperty 
	private Boolean backupLastRunSuccess;
	@JsonProperty
	private Integer backupFileCount;
	@JsonProperty
	private Integer backupHour;
	@JsonProperty
	private Integer backupMinute;
	@JsonProperty
	private Boolean backupEnabled;
	
	/**
	 * Validate the Settings
	 * @param response
	 */
	public void validate(ProcessResult response){
		
		switch(emailContentType){
			case MangoEmailContent.CONTENT_TYPE_BOTH:
			case MangoEmailContent.CONTENT_TYPE_HTML:
			case MangoEmailContent.CONTENT_TYPE_TEXT:
			break;
			default:
				response.addContextualMessage(SystemSettingsDao.EMAIL_CONTENT_TYPE, "validate.invalideValue");

		}
		
		validatePeriodType(pointDataPurgePeriodType,SystemSettingsDao.POINT_DATA_PURGE_PERIOD_TYPE,response);
		validatePeriodType(eventPurgePeriodType, SystemSettingsDao.EVENT_PURGE_PERIOD_TYPE,response);
		
		//Should validate language not sure how yet
		
		try {
            ColorUtils.toColor(chartBackgroundColor);
        }
        catch (InvalidArgumentException e) {
            response.addContextualMessage(SystemSettingsDao.CHART_BACKGROUND_COLOUR,
                    "systemSettings.validation.invalidColour");
        }
		try {
            ColorUtils.toColor(chartBackgroundColor);
        }
        catch (InvalidArgumentException e) {
            response.addContextualMessage(SystemSettingsDao.PLOT_BACKGROUND_COLOUR,
                    "systemSettings.validation.invalidColour");
        }
		try {
            ColorUtils.toColor(plotGridlineColor);
        }
        catch (InvalidArgumentException e) {
            response.addContextualMessage(SystemSettingsDao.PLOT_GRIDLINE_COLOUR,
                    "systemSettings.validation.invalidColour");
        }
		
		//Validate
    	File tmp = new File(backupFileLocation);
    	if(!tmp.exists()){
    		//Doesn't exist, push up message
    		response.addContextualMessage(SystemSettingsDao.BACKUP_FILE_LOCATION,
    				"systemSettings.validation.backupLocationNotExists");
    	}
    	if(!tmp.canWrite()){
    		response.addContextualMessage(SystemSettingsDao.BACKUP_FILE_LOCATION,
    				"systemSettings.validation.cannotWriteToBackupFileLocation");
    	}
    	//Validate the Hour and Minute
    	if((backupHour > 23)||(backupHour<0)){
    		response.addContextualMessage(SystemSettingsDao.BACKUP_HOUR,
    				"systemSettings.validation.backupHourInvalid");
    	}
    	if((backupMinute > 59)||(backupMinute<0)){
    		response.addContextualMessage(SystemSettingsDao.BACKUP_MINUTE,
    				"systemSettings.validation.backupMinuteInvalid");
    	}
    	
    	//Validate the number of backups to keep
    	if(backupFileCount < 1){
    		response.addContextualMessage(SystemSettingsDao.BACKUP_FILE_COUNT,
    				"systemSettings.validation.backupFileCountInvalid");
    	}   	
		
	}
	

	/**
	 * @param pointDataPurgePeriodType2
	 * @param pointDataPurgePeriodType3
	 * @param response
	 */
	private void validatePeriodType(Integer value,
			String contextKey, ProcessResult response) {
		
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
			response.addContextualMessage(contextKey, "validate.invalidValue");
		}
		
	}


	/* (non-Javadoc)
	 * @see com.serotonin.json.spi.JsonSerializable#jsonRead(com.serotonin.json.JsonReader, com.serotonin.json.type.JsonObject)
	 */
	@Override
	public void jsonRead(JsonReader reader, JsonObject json) throws JsonException {
		
	}

	/* (non-Javadoc)
	 * @see com.serotonin.json.spi.JsonSerializable#jsonWrite(com.serotonin.json.ObjectWriter)
	 */
	@Override
	public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
				
	}
	


	public String getDatabaseSchemaVersion() {
		return databaseSchemaVersion;
	}

	public void setDatabaseSchemaVersion(String databaseSchemaVersion) {
		this.databaseSchemaVersion = databaseSchemaVersion;
	}

	public Boolean getNewInstance() {
		return newInstance;
	}

	public void setNewInstance(Boolean newInstance) {
		this.newInstance = newInstance;
	}

	public String getEmailSmtpHost() {
		return emailSmtpHost;
	}

	public void setEmailSmtpHost(String emailSmtpHost) {
		this.emailSmtpHost = emailSmtpHost;
	}

	public Integer getEmailSmtpPort() {
		return emailSmtpPort;
	}

	public void setEmailSmtpPort(Integer emailSmtpPort) {
		this.emailSmtpPort = emailSmtpPort;
	}

	public String getEmailFromAddress() {
		return emailFromAddress;
	}

	public void setEmailFromAddress(String emailFromAddress) {
		this.emailFromAddress = emailFromAddress;
	}

	public String getEmailSmtpUsername() {
		return emailSmtpUsername;
	}

	public void setEmailSmtpUsername(String emailSmtpUsername) {
		this.emailSmtpUsername = emailSmtpUsername;
	}

	public String getEmailSmtpPassword() {
		return emailSmtpPassword;
	}

	public void setEmailSmtpPassword(String emailSmtpPassword) {
		this.emailSmtpPassword = emailSmtpPassword;
	}

	public String getEmailFromName() {
		return emailFromName;
	}

	public void setEmailFromName(String emailFromName) {
		this.emailFromName = emailFromName;
	}

	public Boolean getEmailAuthorization() {
		return emailAuthorization;
	}

	public void setEmailAuthorization(Boolean emailAuthorization) {
		this.emailAuthorization = emailAuthorization;
	}

	public Boolean getEmailTls() {
		return emailTls;
	}

	public void setEmailTls(Boolean emailTls) {
		this.emailTls = emailTls;
	}

	public Integer getEmailContentType() {
		return emailContentType;
	}

	public void setEmailContentType(Integer emailContentType) {
		this.emailContentType = emailContentType;
	}

	public Integer getPointDataPurgePeriodType() {
		return pointDataPurgePeriodType;
	}

	public void setPointDataPurgePeriodType(Integer pointDataPurgePeriodType) {
		this.pointDataPurgePeriodType = pointDataPurgePeriodType;
	}

	public Integer getPointDataPurgePeriods() {
		return pointDataPurgePeriods;
	}

	public void setPointDataPurgePeriods(Integer pointDataPurgePeriods) {
		this.pointDataPurgePeriods = pointDataPurgePeriods;
	}

	public Integer getEventPurgePeriodType() {
		return eventPurgePeriodType;
	}

	public void setEventPurgePeriodType(Integer eventPurgePeriodType) {
		this.eventPurgePeriodType = eventPurgePeriodType;
	}

	public Integer getEventPurgePeriods() {
		return eventPurgePeriods;
	}

	public void setEventPurgePeriods(Integer eventPurgePeriods) {
		this.eventPurgePeriods = eventPurgePeriods;
	}

	public Boolean getHttpClientUseProxy() {
		return httpClientUseProxy;
	}

	public void setHttpClientUseProxy(Boolean httpClientUseProxy) {
		this.httpClientUseProxy = httpClientUseProxy;
	}

	public String getHttpClientProxyServer() {
		return httpClientProxyServer;
	}

	public void setHttpClientProxyServer(String httpClientProxyServer) {
		this.httpClientProxyServer = httpClientProxyServer;
	}

	public Integer getHttpClientProxyPort() {
		return httpClientProxyPort;
	}

	public void setHttpClientProxyPort(Integer httpClientProxyPort) {
		this.httpClientProxyPort = httpClientProxyPort;
	}

	public String getHttpClientProxyUsername() {
		return httpClientProxyUsername;
	}

	public void setHttpClientProxyUsername(String httpClientProxyUsername) {
		this.httpClientProxyUsername = httpClientProxyUsername;
	}

	public String getHttpClientProxyPassword() {
		return httpClientProxyPassword;
	}

	public void setHttpClientProxyPassword(String httpClientProxyPassword) {
		this.httpClientProxyPassword = httpClientProxyPassword;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getFiledataPath() {
		return filedataPath;
	}

	public void setFiledataPath(String filedataPath) {
		this.filedataPath = filedataPath;
	}

	public String getDatasourceDisplaySuffix() {
		return datasourceDisplaySuffix;
	}

	public void setDatasourceDisplaySuffix(String datasourceDisplaySuffix) {
		this.datasourceDisplaySuffix = datasourceDisplaySuffix;
	}

	public String getHttpdsPrologue() {
		return httpdsPrologue;
	}

	public void setHttpdsPrologue(String httpdsPrologue) {
		this.httpdsPrologue = httpdsPrologue;
	}

	public String getHttpdsEpilogue() {
		return httpdsEpilogue;
	}

	public void setHttpdsEpilogue(String httpdsEpilogue) {
		this.httpdsEpilogue = httpdsEpilogue;
	}

	public Integer getUiPerformance() {
		return uiPerformance;
	}

	public void setUiPerformance(Integer uiPerformance) {
		this.uiPerformance = uiPerformance;
	}

	public Integer getFutureDateLimitPeriods() {
		return futureDateLimitPeriods;
	}

	public void setFutureDateLimitPeriods(Integer futureDateLimitPeriods) {
		this.futureDateLimitPeriods = futureDateLimitPeriods;
	}

	public Integer getFutureDateLimitPeriodType() {
		return futureDateLimitPeriodType;
	}

	public void setFutureDateLimitPeriodType(Integer futureDateLimitPeriodType) {
		this.futureDateLimitPeriodType = futureDateLimitPeriodType;
	}

	public String getInstanceDescription() {
		return instanceDescription;
	}

	public void setInstanceDescription(String instanceDescription) {
		this.instanceDescription = instanceDescription;
	}

	public String getChartBackgroundColor() {
		return chartBackgroundColor;
	}

	public void setChartBackgroundColor(String chartBackgroundColor) {
		this.chartBackgroundColor = chartBackgroundColor;
	}

	public String getPlotBackgroundColor() {
		return plotBackgroundColor;
	}

	public void setPlotBackgroundColor(String plotBackgroundColor) {
		this.plotBackgroundColor = plotBackgroundColor;
	}

	public String getPlotGridlineColor() {
		return plotGridlineColor;
	}

	public void setPlotGridlineColor(String plotGridlineColor) {
		this.plotGridlineColor = plotGridlineColor;
	}

	public String getBackupFileLocation() {
		return backupFileLocation;
	}

	public void setBackupFileLocation(String backupFileLocation) {
		this.backupFileLocation = backupFileLocation;
	}

	public Integer getBackupPeriodType() {
		return backupPeriodType;
	}

	public void setBackupPeriodType(Integer backupPeriodType) {
		this.backupPeriodType = backupPeriodType;
	}

	public Integer getBackupPeriods() {
		return backupPeriods;
	}

	public void setBackupPeriods(Integer backupPeriods) {
		this.backupPeriods = backupPeriods;
	}

	public Integer getBackupFileCount() {
		return backupFileCount;
	}

	public void setBackupFileCount(Integer backupFileCount) {
		this.backupFileCount = backupFileCount;
	}

	public Integer getBackupHour() {
		return backupHour;
	}

	public void setBackupHour(Integer backupHour) {
		this.backupHour = backupHour;
	}

	public Integer getBackupMinute() {
		return backupMinute;
	}

	public void setBackupMinute(Integer backupMinute) {
		this.backupMinute = backupMinute;
	}

	public Boolean getBackupEnabled() {
		return backupEnabled;
	}

	public void setBackupEnabled(Boolean backupEnabled) {
		this.backupEnabled = backupEnabled;
	}


	public Boolean getBackupLastRunSuccess() {
		return backupLastRunSuccess;
	}


	public void setBackupLastRunSuccess(Boolean backupLastRunSuccess) {
		this.backupLastRunSuccess = backupLastRunSuccess;
	}

	
	
	
	
	
}
