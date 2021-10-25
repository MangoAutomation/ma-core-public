/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring.components;

import java.time.Period;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.spring.components.DebounceService.Debounce;
import com.infiniteautomation.mango.spring.events.audit.SystemSettingChangeAuditEvent;
import com.serotonin.m2m2.Common.TimePeriods;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;

/**
 * Responsible for setting the retention policy for the time series database.
 */
@Component
public class PurgeSettingsListener {

    private final Logger log = LoggerFactory.getLogger(PurgeSettingsListener.class);
    private final PointValueDao pointValueDao;
    private final SystemSettingsDao systemSettingsDao;
    private final Debounce configureRetentionPolicyDebounce;

    private final Set<String> systemSettingKeys = Set.of(
            SystemSettingsDao.POINT_DATA_PURGE_PERIODS,
            SystemSettingsDao.POINT_DATA_PURGE_PERIOD_TYPE
    );


    @Autowired
    public PurgeSettingsListener(PointValueDao pointValueDao, SystemSettingsDao systemSettingsDao, DebounceService debounceService) {
        this.pointValueDao = pointValueDao;
        this.systemSettingsDao = systemSettingsDao;

        // prevents configureRetentionPolicy() being called multiple times when individual system settings are configured
        this.configureRetentionPolicyDebounce = debounceService.debounce(this::configureRetentionPolicy, 10, TimeUnit.SECONDS);
    }

    @SuppressWarnings("SpringEventListenerInspection")
    @EventListener
    private void systemSettingChangeAuditEventListener(SystemSettingChangeAuditEvent event) {
        if (systemSettingKeys.contains(event.getKey())) {
            configureRetentionPolicyDebounce.run();
        }
    }

    @PostConstruct
    private void configureRetentionPolicy() {
        int periods = systemSettingsDao.getIntValue(SystemSettingsDao.POINT_DATA_PURGE_PERIODS);
        int type = systemSettingsDao.getIntValue(SystemSettingsDao.POINT_DATA_PURGE_PERIOD_TYPE);

        if (periods <= 0) {
            log.warn("Purge periods should greater than zero. Retention policy will not be configured.");
            return;
        }

        Period period;
        try {
            period = TimePeriods.toPeriod(type, periods);
        } catch (IllegalArgumentException e) {
            log.warn("Unsupported purge period type, should be days, weeks, months or years. Retention policy will not be configured.");
            return;
        }

        try {
            pointValueDao.setRetentionPolicy(period);
            log.info("Retention policy of {} configured", period);
        } catch (UnsupportedOperationException e) {
            log.info("Database does not support setting retention policy");
        }
    }
}
