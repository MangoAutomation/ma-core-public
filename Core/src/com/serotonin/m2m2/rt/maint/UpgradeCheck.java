/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.maint;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.infiniteautomation.mango.monitor.ValueMonitor;
import com.infiniteautomation.mango.spring.service.ModulesService;
import com.infiniteautomation.mango.util.exception.FeatureDisabledException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.ReturnCause;
import com.serotonin.m2m2.rt.event.type.DuplicateHandling;
import com.serotonin.m2m2.rt.event.type.SystemEventType;
import com.serotonin.timer.FixedRateTrigger;
import com.serotonin.timer.TimerTask;

/**
 * @author Matthew Lohbihler
 */
public class UpgradeCheck extends TimerTask {

    private final Log log = LogFactory.getLog(UpgradeCheck.class);
    private static final long DELAY_TIMEOUT = 1000 * 10; // Run initially after 10 seconds
    private static final long PERIOD_TIMEOUT = 1000 * 60 * 60 * 24; // Run every 24 hours.

    public static final String UPGRADES_AVAILABLE_MONITOR_ID = "com.serotonin.m2m2.rt.maint.UpgradeCheck.COUNT";
    private final ValueMonitor<Integer> availableUpgrades;

    /**
     * This method will set up the upgrade checking job. It assumes that the corresponding system setting for running
     * this job is true.
     */
    public static void start() {
        Common.backgroundProcessing.schedule(new UpgradeCheck());
    }

    private final SystemEventType et = new SystemEventType(SystemEventType.TYPE_UPGRADE_CHECK, 0,
            DuplicateHandling.IGNORE);

    public UpgradeCheck() {
        super(new FixedRateTrigger(DELAY_TIMEOUT, PERIOD_TIMEOUT), "Upgrade check task", "UpgradeCheck", 0);

        this.availableUpgrades = Common.MONITORED_VALUES.<Integer>create(UPGRADES_AVAILABLE_MONITOR_ID)
                .name(new TranslatableMessage("internal.monitor.AVAILABLE_UPGRADE_COUNT"))
                .build();
    }

    @Override
    public void run(long fireTime) {
        Integer available = null;
        try {
            //If upgrade checks are not enabled we won't contact the store at all
            if(SystemSettingsDao.instance.getBooleanValue(SystemSettingsDao.UPGRADE_CHECKS_ENABLED)) {
                try {
                    available = Common.getBean(ModulesService.class).upgradesAvailable();
                } catch (FeatureDisabledException e) {
                    // ignore
                }
            }

            if (available != null && available > 0) {
                TranslatableMessage m = new TranslatableMessage("modules.event.upgrades");
                SystemEventType.raiseEvent(et, Common.timer.currentTimeMillis(), true, m);
            }
            else
                Common.eventManager.returnToNormal(et, Common.timer.currentTimeMillis(),
                        ReturnCause.RETURN_TO_NORMAL);
        }
        catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            //To ensure that the monitor is set, even if it is with a null value.
            this.availableUpgrades.setValue(available);
        }
    }
}
