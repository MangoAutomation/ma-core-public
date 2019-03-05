/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.spring.security;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.springframework.context.ApplicationContext;
import org.springframework.security.web.context.support.SecurityWebApplicationContextUtils;
import org.springframework.security.web.session.HttpSessionCreatedEvent;
import org.springframework.security.web.session.HttpSessionDestroyedEvent;

import com.infiniteautomation.mango.spring.events.MangoHttpSessionDestroyedEvent;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.vo.systemSettings.SystemSettingsListener;

/**
 *
 * Nice place to modify an HTTP session, i.e. set the timeout.
 *
 * <p>Also performs the work of {@link org.springframework.security.web.session.HttpSessionEventPublisher HttpSessionEventPublisher}</p>
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

    ApplicationContext getContext(ServletContext servletContext) {
        return SecurityWebApplicationContextUtils.findRequiredWebApplicationContext(servletContext);
    }

    @Override
    public void sessionCreated(HttpSessionEvent event) {
        event.getSession().setMaxInactiveInterval(timeoutSeconds);

        HttpSessionCreatedEvent e = new HttpSessionCreatedEvent(event.getSession());
        getContext(event.getSession().getServletContext()).publishEvent(e);
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        HttpSessionDestroyedEvent e = new MangoHttpSessionDestroyedEvent(event.getSession());
        getContext(event.getSession().getServletContext()).publishEvent(e);
    }

    @Override
    public void systemSettingsSaved(String key, String oldValue, String newValue) {
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

    @Override
    public void systemSettingsRemoved(String key, String lastValue, String defaultValue) {

    }

    @Override
    public List<String> getKeys() {
        List<String> keys = new ArrayList<>();
        keys.add(SystemSettingsDao.HTTP_SESSION_TIMEOUT_PERIOD_TYPE);
        keys.add(SystemSettingsDao.HTTP_SESSION_TIMEOUT_PERIODS);
        return keys;
    }
}
