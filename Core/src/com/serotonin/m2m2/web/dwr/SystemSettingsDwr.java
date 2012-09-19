/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.dwr;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.serotonin.InvalidArgumentException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.EventDao;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.email.MangoEmailContent;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.rt.event.type.SystemEventType;
import com.serotonin.m2m2.rt.maint.DataPurge;
import com.serotonin.m2m2.rt.maint.work.EmailWorkItem;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.bean.PointHistoryCount;
import com.serotonin.m2m2.vo.pair.StringIntPair;
import com.serotonin.m2m2.web.dwr.util.DwrPermission;
import com.serotonin.util.ColorUtils;
import com.serotonin.util.DirectoryInfo;
import com.serotonin.util.DirectoryUtils;

public class SystemSettingsDwr extends BaseDwr {
    @DwrPermission(admin = true)
    public Map<String, Object> getSettings() {
        Map<String, Object> settings = new HashMap<String, Object>();

        // Info
        settings.put(SystemSettingsDao.INSTANCE_DESCRIPTION,
                SystemSettingsDao.getValue(SystemSettingsDao.INSTANCE_DESCRIPTION));

        // Email
        settings.put(SystemSettingsDao.EMAIL_SMTP_HOST, SystemSettingsDao.getValue(SystemSettingsDao.EMAIL_SMTP_HOST));
        settings.put(SystemSettingsDao.EMAIL_SMTP_PORT,
                SystemSettingsDao.getIntValue(SystemSettingsDao.EMAIL_SMTP_PORT));
        settings.put(SystemSettingsDao.EMAIL_FROM_ADDRESS,
                SystemSettingsDao.getValue(SystemSettingsDao.EMAIL_FROM_ADDRESS));
        settings.put(SystemSettingsDao.EMAIL_FROM_NAME, SystemSettingsDao.getValue(SystemSettingsDao.EMAIL_FROM_NAME));
        settings.put(SystemSettingsDao.EMAIL_AUTHORIZATION,
                SystemSettingsDao.getBooleanValue(SystemSettingsDao.EMAIL_AUTHORIZATION));
        settings.put(SystemSettingsDao.EMAIL_SMTP_USERNAME,
                SystemSettingsDao.getValue(SystemSettingsDao.EMAIL_SMTP_USERNAME));
        settings.put(SystemSettingsDao.EMAIL_SMTP_PASSWORD,
                SystemSettingsDao.getValue(SystemSettingsDao.EMAIL_SMTP_PASSWORD));
        settings.put(SystemSettingsDao.EMAIL_TLS, SystemSettingsDao.getBooleanValue(SystemSettingsDao.EMAIL_TLS));
        settings.put(SystemSettingsDao.EMAIL_CONTENT_TYPE,
                SystemSettingsDao.getIntValue(SystemSettingsDao.EMAIL_CONTENT_TYPE));

        // System event types
        settings.put("systemEventTypes", SystemEventType.EVENT_TYPES);

        // System event types
        settings.put("auditEventTypes", AuditEventType.EVENT_TYPES);

        // Http
        settings.put(SystemSettingsDao.HTTP_CLIENT_USE_PROXY,
                SystemSettingsDao.getBooleanValue(SystemSettingsDao.HTTP_CLIENT_USE_PROXY));
        settings.put(SystemSettingsDao.HTTP_CLIENT_PROXY_SERVER,
                SystemSettingsDao.getValue(SystemSettingsDao.HTTP_CLIENT_PROXY_SERVER));
        settings.put(SystemSettingsDao.HTTP_CLIENT_PROXY_PORT,
                SystemSettingsDao.getIntValue(SystemSettingsDao.HTTP_CLIENT_PROXY_PORT));
        settings.put(SystemSettingsDao.HTTP_CLIENT_PROXY_USERNAME,
                SystemSettingsDao.getValue(SystemSettingsDao.HTTP_CLIENT_PROXY_USERNAME));
        settings.put(SystemSettingsDao.HTTP_CLIENT_PROXY_PASSWORD,
                SystemSettingsDao.getValue(SystemSettingsDao.HTTP_CLIENT_PROXY_PASSWORD));

        // Misc
        settings.put(SystemSettingsDao.POINT_DATA_PURGE_PERIOD_TYPE,
                SystemSettingsDao.getIntValue(SystemSettingsDao.POINT_DATA_PURGE_PERIOD_TYPE));
        settings.put(SystemSettingsDao.POINT_DATA_PURGE_PERIODS,
                SystemSettingsDao.getIntValue(SystemSettingsDao.POINT_DATA_PURGE_PERIODS));
        settings.put(SystemSettingsDao.EVENT_PURGE_PERIOD_TYPE,
                SystemSettingsDao.getIntValue(SystemSettingsDao.EVENT_PURGE_PERIOD_TYPE));
        settings.put(SystemSettingsDao.EVENT_PURGE_PERIODS,
                SystemSettingsDao.getIntValue(SystemSettingsDao.EVENT_PURGE_PERIODS));
        settings.put(SystemSettingsDao.UI_PERFORMANCE, SystemSettingsDao.getIntValue(SystemSettingsDao.UI_PERFORMANCE));
        settings.put(SystemSettingsDao.FUTURE_DATE_LIMIT_PERIOD_TYPE,
                SystemSettingsDao.getIntValue(SystemSettingsDao.FUTURE_DATE_LIMIT_PERIOD_TYPE));
        settings.put(SystemSettingsDao.FUTURE_DATE_LIMIT_PERIODS,
                SystemSettingsDao.getIntValue(SystemSettingsDao.FUTURE_DATE_LIMIT_PERIODS));

        // Language
        settings.put(SystemSettingsDao.LANGUAGE, SystemSettingsDao.getValue(SystemSettingsDao.LANGUAGE));

        // Colours
        settings.put(SystemSettingsDao.CHART_BACKGROUND_COLOUR,
                SystemSettingsDao.getValue(SystemSettingsDao.CHART_BACKGROUND_COLOUR));
        settings.put(SystemSettingsDao.PLOT_BACKGROUND_COLOUR,
                SystemSettingsDao.getValue(SystemSettingsDao.PLOT_BACKGROUND_COLOUR));
        settings.put(SystemSettingsDao.PLOT_GRIDLINE_COLOUR,
                SystemSettingsDao.getValue(SystemSettingsDao.PLOT_GRIDLINE_COLOUR));

        return settings;
    }

    @DwrPermission(admin = true)
    public Map<String, Object> getDatabaseSize() {
        Map<String, Object> data = new HashMap<String, Object>();

        // Database size
        File dataDirectory = Common.databaseProxy.getDataDirectory();
        long dbSize = 0;
        if (dataDirectory != null) {
            DirectoryInfo dbInfo = DirectoryUtils.getDirectorySize(dataDirectory);
            dbSize = dbInfo.getSize();
            data.put("databaseSize", DirectoryUtils.bytesDescription(dbSize));
        }
        else
            data.put("databaseSize", "(" + translate("common.unknown") + ")");

        // Filedata data
        DirectoryInfo fileDatainfo = DirectoryUtils.getDirectorySize(new File(Common.getFiledataPath()));
        long filedataSize = fileDatainfo.getSize();
        data.put("filedataCount", fileDatainfo.getCount());
        data.put("filedataSize", DirectoryUtils.bytesDescription(filedataSize));

        data.put("totalSize", DirectoryUtils.bytesDescription(dbSize + filedataSize));

        // Point history counts.
        List<PointHistoryCount> counts = new DataPointDao().getTopPointHistoryCounts();
        int sum = 0;
        for (PointHistoryCount c : counts)
            sum += c.getCount();

        data.put("historyCount", sum);
        data.put("topPoints", counts);
        data.put("eventCount", new EventDao().getEventCount());

        return data;
    }

    @DwrPermission(admin = true)
    public void saveEmailSettings(String host, int port, String from, String name, boolean auth, String username,
            String password, boolean tls, int contentType) {
        SystemSettingsDao systemSettingsDao = new SystemSettingsDao();
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

    @DwrPermission(admin = true)
    public Map<String, Object> sendTestEmail(String host, int port, String from, String name, boolean auth,
            String username, String password, boolean tls, int contentType) {
        // Save the settings
        saveEmailSettings(host, port, from, name, auth, username, password, tls, contentType);

        // Get the web context information
        User user = Common.getUser();

        Map<String, Object> result = new HashMap<String, Object>();
        try {
            Translations translations = getTranslations();
            Map<String, Object> model = new HashMap<String, Object>();
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
    public void saveSystemEventAlarmLevels(List<StringIntPair> eventAlarmLevels) {
        for (StringIntPair eventAlarmLevel : eventAlarmLevels)
            SystemEventType.setEventTypeAlarmLevel(eventAlarmLevel.getString(), eventAlarmLevel.getInt());
    }

    @DwrPermission(admin = true)
    public void saveAuditEventAlarmLevels(List<StringIntPair> eventAlarmLevels) {
        for (StringIntPair eventAlarmLevel : eventAlarmLevels)
            AuditEventType.setEventTypeAlarmLevel(eventAlarmLevel.getString(), eventAlarmLevel.getInt());
    }

    @DwrPermission(admin = true)
    public void saveHttpSettings(boolean useProxy, String host, int port, String username, String password) {
        SystemSettingsDao systemSettingsDao = new SystemSettingsDao();
        systemSettingsDao.setBooleanValue(SystemSettingsDao.HTTP_CLIENT_USE_PROXY, useProxy);
        systemSettingsDao.setValue(SystemSettingsDao.HTTP_CLIENT_PROXY_SERVER, host);
        systemSettingsDao.setIntValue(SystemSettingsDao.HTTP_CLIENT_PROXY_PORT, port);
        systemSettingsDao.setValue(SystemSettingsDao.HTTP_CLIENT_PROXY_USERNAME, username);
        systemSettingsDao.setValue(SystemSettingsDao.HTTP_CLIENT_PROXY_PASSWORD, password);
    }

    @DwrPermission(admin = true)
    public void saveMiscSettings(int pointDataPurgePeriodType, int pointDataPurgePeriods, int eventPurgePeriodType,
            int eventPurgePeriods, int uiPerformance, int futureDateLimitPeriodType, int futureDateLimitPeriods) {
        SystemSettingsDao systemSettingsDao = new SystemSettingsDao();
        systemSettingsDao.setIntValue(SystemSettingsDao.POINT_DATA_PURGE_PERIOD_TYPE, pointDataPurgePeriodType);
        systemSettingsDao.setIntValue(SystemSettingsDao.POINT_DATA_PURGE_PERIODS, pointDataPurgePeriods);
        systemSettingsDao.setIntValue(SystemSettingsDao.EVENT_PURGE_PERIOD_TYPE, eventPurgePeriodType);
        systemSettingsDao.setIntValue(SystemSettingsDao.EVENT_PURGE_PERIODS, eventPurgePeriods);
        systemSettingsDao.setIntValue(SystemSettingsDao.UI_PERFORMANCE, uiPerformance);
        systemSettingsDao.setIntValue(SystemSettingsDao.FUTURE_DATE_LIMIT_PERIOD_TYPE, futureDateLimitPeriodType);
        systemSettingsDao.setIntValue(SystemSettingsDao.FUTURE_DATE_LIMIT_PERIODS, futureDateLimitPeriods);
    }

    @DwrPermission(admin = true)
    public void saveInfoSettings(String instanceDescription) {
        SystemSettingsDao systemSettingsDao = new SystemSettingsDao();
        systemSettingsDao.setValue(SystemSettingsDao.INSTANCE_DESCRIPTION, instanceDescription);
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
            SystemSettingsDao systemSettingsDao = new SystemSettingsDao();
            systemSettingsDao.setValue(SystemSettingsDao.CHART_BACKGROUND_COLOUR, chartBackgroundColour);
            systemSettingsDao.setValue(SystemSettingsDao.PLOT_BACKGROUND_COLOUR, plotBackgroundColour);
            systemSettingsDao.setValue(SystemSettingsDao.PLOT_GRIDLINE_COLOUR, plotGridlineColour);
        }

        return response;
    }

    @DwrPermission(admin = true)
    public void saveLanguageSettings(String language) {
        SystemSettingsDao systemSettingsDao = new SystemSettingsDao();
        systemSettingsDao.setValue(SystemSettingsDao.LANGUAGE, language);
        Common.setSystemLanguage(language);
    }

    @DwrPermission(admin = true)
    public void purgeNow() {
        DataPurge dataPurge = new DataPurge();
        dataPurge.execute(System.currentTimeMillis());
    }

    @DwrPermission(admin = true)
    public TranslatableMessage purgeAllData() {
        long cnt = Common.runtimeManager.purgeDataPointValues();
        return new TranslatableMessage("systemSettings.purgeDataComplete", cnt);
    }

    @DwrPermission(admin = true)
    public void saveSettings(Map<String, String> settings) {
        SystemSettingsDao systemSettingsDao = new SystemSettingsDao();

        for (Map.Entry<String, String> entry : settings.entrySet())
            systemSettingsDao.setValue(entry.getKey(), entry.getValue());
    }
}
