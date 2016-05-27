/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.maint;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.EventInstance;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.rt.event.type.SystemEventType;
import com.serotonin.m2m2.util.timeout.RejectableTimerTask;
import com.serotonin.m2m2.web.dwr.ModulesDwr;
import com.serotonin.timer.FixedRateTrigger;

/**
 * @author Matthew Lohbihler
 */
public class UpgradeCheck extends RejectableTimerTask {
    private static final Log LOG = LogFactory.getLog(UpgradeCheck.class);
    private static final long DELAY_TIMEOUT = 1000 * 10; // Run initially after 10 seconds
    private static final long PERIOD_TIMEOUT = 1000 * 60 * 60 * 4; // Run every 4 hours.

    /**
     * This method will set up the upgrade checking job. It assumes that the corresponding system setting for running
     * this job is true.
     */
    public static void start() {
        Common.backgroundProcessing.schedule(new UpgradeCheck());
    }

    private final SystemEventType et = new SystemEventType(SystemEventType.TYPE_UPGRADE_CHECK, 0,
            EventType.DuplicateHandling.IGNORE);

    public UpgradeCheck() {
        super(new FixedRateTrigger(DELAY_TIMEOUT, PERIOD_TIMEOUT), "Upgrade check task", "UpgradeCheck", 0, true);
    }

    @Override
    public void run(long fireTime) {
        try {
            if (ModulesDwr.isUpgradeAvailable()) {
                TranslatableMessage m = new TranslatableMessage("modules.event.upgrades");
                SystemEventType.raiseEvent(et, Common.backgroundProcessing.currentTimeMillis(), true, m);
            }
            else
                Common.eventManager.returnToNormal(et, Common.backgroundProcessing.currentTimeMillis(),
                        EventInstance.RtnCauses.RETURN_TO_NORMAL);
        }
        catch (Exception e) {
            LOG.error(e);
        }
    }
}
