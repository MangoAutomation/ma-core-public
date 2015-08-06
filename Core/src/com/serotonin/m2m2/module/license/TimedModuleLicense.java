package com.serotonin.m2m2.module.license;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.LicenseDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.rt.event.type.SystemEventType;
import com.serotonin.m2m2.util.license.ModuleLicense;
import com.serotonin.m2m2.vo.event.EventTypeVO;
import com.serotonin.timer.FixedRateTrigger;
import com.serotonin.timer.TimerTask;

/**
 * Simply checks that a license for the module exists and stops the timer if so.
 */
public abstract class TimedModuleLicense extends LicenseDefinition {
	
	private final Log LOG = LogFactory.getLog(TimedModuleLicense.class);

	protected TimerTask task;

	protected ExpiryAction action;
	protected long timeoutRecheckPeriod; //Check for action every so often
	protected int runtime; //Time to run before action takes place
	protected String messageKey; //Message key for message with {0} that represents the time at which the timeout will occur
	
	/**
	 * 
	 * @param action
	 * @param timeoutRecheckPeriods
	 * @param timeoutRecheckPeriodType
	 * @param runtimePeriods
	 * @param runtimePeriodType
	 * @param messageKey
	 */
	public TimedModuleLicense(ExpiryAction action, int timeoutRecheckPeriods, int timeoutRecheckPeriodType, int runtimePeriods, int runtimePeriodType, String messageKey){
		this.action = action;
		this.timeoutRecheckPeriod = Common.getMillis(timeoutRecheckPeriodType, timeoutRecheckPeriods);
		this.runtime = (int)Common.getMillis(runtimePeriodType, runtimePeriods);
		this.messageKey = messageKey;
	}
	
    private final List<TranslatableMessage> ERRORS = new ArrayList<>();

    @Override
    public void addLicenseErrors(List<TranslatableMessage> errors) {
        errors.addAll(ERRORS);
    }

    @Override
    public void addLicenseWarnings(List<TranslatableMessage> warnings) {
        // no op
    }

    @Override
    public void licenseCheck(boolean initialization) {
        if (initialization) {
            ERRORS.clear();
            if(shouldScheduleTimeout()&&(this.task == null)){
            	SimpleDateFormat sdf = new SimpleDateFormat("MMM dd yyyy HH:mm:ss");
            	Date shutdownTime = new Date(System.currentTimeMillis() + this.runtime);
            	final String shutdownString = sdf.format(shutdownTime);
    			LOG.warn(getModule().getName() + " is unlicensed, system will shutdown at " + shutdownString);
            	
                ERRORS.add(new TranslatableMessage("module.notLicensed"));
                
                final TimedExpiryChecker checker = new TimedExpiryChecker(getModule().getName(), this.runtime, this.action);
                ModuleRegistry.addLicenseEnforcement(checker);
                
        		this.task = new TimerTask(new FixedRateTrigger(timeoutRecheckPeriod, timeoutRecheckPeriod)){
        			private boolean alarmRaised = false;
        			private SystemEventType eventType = new SystemEventType(SystemEventType.TYPE_LICENSE_CHECK, 0,
	    	                EventType.DuplicateHandling.ALLOW);
        			private TimedExpiryChecker tec = checker; 
        			@Override
        			public void run(long runtime) {
        				tec.isExpired();
        				//Raise a warning the first time we check.
        				if(!alarmRaised){
        					
        					//Ensure our events subsystem is active
        					EventTypeVO vo  = SystemEventType.getEventType(eventType.getSystemEventType());
        					if(vo == null)
        						return; //We can't raise an event yet
        					
        					if(modifyTaskWhenSystemReady(this)){
	        					alarmRaised = true;
	        	    			SystemEventType.raiseEvent(eventType, System.currentTimeMillis(), true,
	        	    	                new TranslatableMessage(messageKey, shutdownString));
        	    			}

        				}
        			}
        			
        		};
                Common.timer.schedule(task);
            }else{
            	if(task != null)
	        		task.cancel();
            }
        }else{
        	if(!shouldScheduleTimeout())
	        	if(task != null)
	        		task.cancel();
        }
    }
    
    /**
     * Method to be overridden if something other than no license or a free licenes should schedule the shutdown.
     * 
     * Be careful what is done here as the DB, Event Manager and Runtime Manager are not available yet
     * @return
     */
    protected boolean shouldScheduleTimeout(){
    	//Check the license from the registry in case it changed 
    	ModuleLicense license = ModuleRegistry.getModule(this.getModule().getName()).license();
    	return (license == null) || (license.getLicenseType().equals("free"));
    }
    
    /**
     * Override if system is ready
     * @return true if we should raise the alarm once
     */
    protected boolean modifyTaskWhenSystemReady(TimerTask task){
    	return true;
    }
}
