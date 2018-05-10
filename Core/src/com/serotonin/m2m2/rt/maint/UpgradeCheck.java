/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.maint;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.infiniteautomation.mango.monitor.IntegerMonitor;
import com.infiniteautomation.mango.monitor.ValueMonitorOwner;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.EventInstance;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.rt.event.type.SystemEventType;
import com.serotonin.m2m2.web.dwr.ModulesDwr;
import com.serotonin.timer.FixedRateTrigger;
import com.serotonin.timer.TimerTask;

/**
 * @author Matthew Lohbihler
 */
public class UpgradeCheck extends TimerTask implements ValueMonitorOwner{
	
    private static final Log LOG = LogFactory.getLog(UpgradeCheck.class);
    private static final long DELAY_TIMEOUT = 1000 * 10; // Run initially after 10 seconds
    private static final long PERIOD_TIMEOUT = 1000 * 60 * 60 * 24; // Run every 24 hours.

    public static final String UPGRADES_AVAILABLE_MONITOR_ID = "com.serotonin.m2m2.rt.maint.UpgradeCheck.COUNT";
    private IntegerMonitor availableUpgrades;
    
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
        super(new FixedRateTrigger(DELAY_TIMEOUT, PERIOD_TIMEOUT), "Upgrade check task", "UpgradeCheck", 0);
    }

    @Override
    public void run(long fireTime) {
        try {
        	int available = ModulesDwr.upgradesAvailable();
            if (available > 0) {
                TranslatableMessage m = new TranslatableMessage("modules.event.upgrades");
                SystemEventType.raiseEvent(et, Common.timer.currentTimeMillis(), true, m);
            }
            else
                Common.eventManager.returnToNormal(et, Common.timer.currentTimeMillis(),
                        EventInstance.RtnCauses.RETURN_TO_NORMAL, AlarmLevels.URGENT);
            if(this.availableUpgrades == null) {
                this.availableUpgrades = new IntegerMonitor(UPGRADES_AVAILABLE_MONITOR_ID, new TranslatableMessage("internal.monitor.AVAILABLE_UPGRADE_COUNT"), this, available);
                Common.MONITORED_VALUES.addIfMissingStatMonitor(this.availableUpgrades);
            } else
                this.availableUpgrades.setValue(available);
        }
        catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

	/* (non-Javadoc)
	 * @see com.infiniteautomation.mango.monitor.ValueMonitorOwner#reset(java.lang.String)
	 */
	@Override
	public void reset(String monitorId) {
	    
	}
}
