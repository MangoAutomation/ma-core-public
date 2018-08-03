/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.spring.security;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.vo.systemSettings.SystemSettingsListener;

/**
 * 
 * Nice place to modify an HTTP session, i.e. set the timeout.
 *
 * @author Terry Packer
 */
public class MangoSessionListener implements HttpSessionListener, SystemSettingsListener {
    
    private int timeoutPeriods;
    private int timeoutPeriodType;
    private int timeoutSeconds;

    public MangoSessionListener() {
        timeoutPeriods = SystemSettingsDao.instance.getIntValue(SystemSettingsDao.HTTP_SESSION_TIMEOUT_PERIODS);
        timeoutPeriodType = SystemSettingsDao.instance.getIntValue(SystemSettingsDao.HTTP_SESSION_TIMEOUT_PERIOD_TYPE);
        timeoutSeconds = (int)(Common.getMillis(timeoutPeriodType, timeoutPeriods)/1000L);
    }
    
    /* (non-Javadoc)
     * @see javax.servlet.http.HttpSessionListener#sessionCreated(javax.servlet.http.HttpSessionEvent)
     */
    @Override
    public void sessionCreated(HttpSessionEvent se) {
        se.getSession().setMaxInactiveInterval(timeoutSeconds);
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpSessionListener#sessionDestroyed(javax.servlet.http.HttpSessionEvent)
     */
    @Override
    public void sessionDestroyed(HttpSessionEvent se) {    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.vo.systemSettings.SystemSettingsListener#SystemSettingsSaved(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void SystemSettingsSaved(String key, String oldValue, String newValue) {
        switch(key) {
            case SystemSettingsDao.HTTP_SESSION_TIMEOUT_PERIOD_TYPE:
                try {
                    timeoutPeriodType = Integer.parseInt(newValue);
                }catch(Exception e) {
                    //Must be a code
                    timeoutPeriodType = Common.TIME_PERIOD_CODES.getId(newValue);
                }
            break;
            case SystemSettingsDao.HTTP_SESSION_TIMEOUT_PERIODS:
                timeoutPeriods = Integer.parseInt(newValue);
            break;
        }
        timeoutSeconds = (int)(Common.getMillis(timeoutPeriodType, timeoutPeriods)/1000L);
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.vo.systemSettings.SystemSettingsListener#SystemSettingsRemoved(java.lang.String, java.lang.String)
     */
    @Override
    public void SystemSettingsRemoved(String key, String lastValue) {
        
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.vo.systemSettings.SystemSettingsListener#getKeys()
     */
    @Override
    public List<String> getKeys() {
        List<String> keys = new ArrayList<>();
        keys.add(SystemSettingsDao.HTTP_SESSION_TIMEOUT_PERIOD_TYPE);
        keys.add(SystemSettingsDao.HTTP_SESSION_TIMEOUT_PERIODS);
        return keys;
    }

}
