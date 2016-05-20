package com.serotonin.m2m2.module.license;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.LicenseDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.util.license.ModuleLicense;
import com.serotonin.m2m2.util.timeout.RejectableTimerTask;
import com.serotonin.timer.FixedRateTrigger;
import com.serotonin.timer.TimerTask;

/**
 * Simply checks that a license for the module exists and stops the timer if so.
 */
public abstract class TimedModuleLicense extends LicenseDefinition {

	protected RejectableTimerTask task;

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
            	Date shutdownTime = new Date(Common.backgroundProcessing.currentTimeMillis() + this.runtime);
            	final String shutdownString = sdf.format(shutdownTime);
    			
            	
                ERRORS.add(new TranslatableMessage("module.notLicensed"));
                
                final TimedExpiryChecker checker = new TimedExpiryChecker(getModule().getName(), this.runtime, this.action);
                ModuleRegistry.addLicenseEnforcement(checker);
                
        		this.task = new TimedLicenseEnforcementChecker(
        				new FixedRateTrigger(timeoutRecheckPeriod, timeoutRecheckPeriod),
        				"License Enforcement Checker for " + getModule().getName(),
        				this,
        				checker,
        				messageKey,
        				shutdownString
        				);
        		
                Common.backgroundProcessing.schedule(task);
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
