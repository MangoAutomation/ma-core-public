/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.spring.security;

import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.web.session.HttpSessionCreatedEvent;
import org.springframework.security.web.session.HttpSessionDestroyedEvent;
import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.spring.events.MangoHttpSessionDestroyedEvent;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.vo.systemSettings.SystemSettingsListener;

/**
 *
 * Nice place to modify an HTTP session, i.e. set the timeout.
 *
 * Replaces {@link org.springframework.security.web.session.HttpSessionEventPublisher HttpSessionEventPublisher} as there is a bug in Spring which prevents getting the Authentication from the session attribute
 * with an asynchronous event publisher
 *
 * @author Terry Packer
 */
@Component
public class MangoSessionListener implements HttpSessionListener, SystemSettingsListener {

    private volatile int timeoutPeriods;
    private volatile int timeoutPeriodType;
    private volatile int timeoutSeconds;
    private final SystemSettingsDao systemSettingsDao;
    private final ApplicationContext context;

    @Autowired
    private MangoSessionListener(SystemSettingsDao systemSettingsDao, ApplicationContext context) {
        this.systemSettingsDao = systemSettingsDao;
        this.context = context;
    }

    @PostConstruct
    public void initialize() {
        timeoutPeriods = this.systemSettingsDao.getIntValue(SystemSettingsDao.HTTP_SESSION_TIMEOUT_PERIODS);
        timeoutPeriodType = this.systemSettingsDao.getIntValue(SystemSettingsDao.HTTP_SESSION_TIMEOUT_PERIOD_TYPE);
        this.updateTimeoutSeconds();
    }

    @Override
    public void sessionCreated(HttpSessionEvent event) {
        event.getSession().setMaxInactiveInterval(timeoutSeconds);

        HttpSessionCreatedEvent e = new HttpSessionCreatedEvent(event.getSession());
        context.publishEvent(e);
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        HttpSessionDestroyedEvent e = new MangoHttpSessionDestroyedEvent(event.getSession());
        context.publishEvent(e);
    }

    @Override
    public void systemSettingsSaved(String key, String oldValue, String newValue) {
        this.settingChanged(key, newValue);
    }

    @Override
    public void systemSettingsRemoved(String key, String lastValue, String defaultValue) {
        this.settingChanged(key, defaultValue);
    }

    private void settingChanged(String key, String newValue) {
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
        this.updateTimeoutSeconds();
    }

    private void updateTimeoutSeconds() {
        timeoutSeconds = (int) (Common.getMillis(timeoutPeriodType, timeoutPeriods) / 1000L);
    }

    @Override
    public List<String> getKeys() {
        return Arrays.asList(
                SystemSettingsDao.HTTP_SESSION_TIMEOUT_PERIOD_TYPE,
                SystemSettingsDao.HTTP_SESSION_TIMEOUT_PERIODS);
    }

}
