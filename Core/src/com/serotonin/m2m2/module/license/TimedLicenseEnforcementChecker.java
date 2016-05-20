/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.module.license;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.rt.event.type.SystemEventType;
import com.serotonin.m2m2.util.timeout.RejectableTimerTask;
import com.serotonin.m2m2.vo.event.EventTypeVO;
import com.serotonin.timer.TimerTrigger;

/**
 * Class that periodically checks to see if the license is invalid
 * 
 * 
 * @author Terry Packer
 *
 */
public class TimedLicenseEnforcementChecker extends RejectableTimerTask{

	private final Log LOG = LogFactory.getLog(TimedLicenseEnforcementChecker.class);
	
	private boolean alarmRaised = false;
	private SystemEventType eventType;
	
	private TimedModuleLicense license;
	private TimedExpiryChecker tec; 
	private String messageKey;
	private String shutdownString;

	
	/**
	 * @param trigger
	 * @param name
	 */
	public TimedLicenseEnforcementChecker(TimerTrigger trigger, String name, TimedModuleLicense license, TimedExpiryChecker checker, String messageKey, String shutdownString) {
		super(trigger, name, null, 0);
		
		this.license = license;
		this.tec = checker;
		this.messageKey = messageKey;
		this.shutdownString = shutdownString;
		
		this.eventType = new SystemEventType(SystemEventType.TYPE_LICENSE_CHECK, 0,
	            EventType.DuplicateHandling.ALLOW);
		
	}

	
	
	@Override
	public void run(long runtime) {
		tec.isExpired();
		//Raise a warning the first time we check.
		if(!alarmRaised){
			
			//Ensure our events subsystem is active
			EventTypeVO vo  = SystemEventType.getEventType(eventType.getSystemEventType());
			if(vo == null)
				return; //We can't raise an event yet
			
			if(license.modifyTaskWhenSystemReady(this)){
				alarmRaised = true;
    			SystemEventType.raiseEvent(eventType, Common.backgroundProcessing.currentTimeMillis(), true,
    	                new TranslatableMessage(messageKey, shutdownString));
    			LOG.warn(license.getModule().getName() + " is unlicensed, system will shutdown at " + shutdownString);
			}

		}
	}
}
