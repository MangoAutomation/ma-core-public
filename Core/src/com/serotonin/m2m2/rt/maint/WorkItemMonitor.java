/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.maint;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

import com.infiniteautomation.mango.monitor.DoubleMonitor;
import com.infiniteautomation.mango.monitor.IntegerMonitor;
import com.infiniteautomation.mango.monitor.ValueMonitorOwner;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.web.mvc.spring.security.MangoSecurityConfiguration;
import com.serotonin.m2m2.web.mvc.spring.security.MangoSessionRegistry;
import com.serotonin.timer.FixedRateTrigger;
import com.serotonin.timer.TimerTask;

public class WorkItemMonitor extends TimerTask implements ValueMonitorOwner {
    private static final long TIMEOUT = 1000 * 10; // Run every ten seconds.

    /**
     * This method will set up the memory checking job. It assumes that the corresponding system setting for running
     * this job is true.
     */
    public static void start() {
        Common.backgroundProcessing.schedule(new WorkItemMonitor());
    }

    public static final String MAX_STACK_HEIGHT_MONITOR_ID = WorkItemMonitor.class.getName() + ".maxStackHeight";
    public static final String THREAD_COUNT_MONITOR_ID = WorkItemMonitor.class.getName() + ".threadCount";
    public static final String DB_ACTIVE_CONNECTIONS_MONITOR_ID = WorkItemMonitor.class.getName()
            + ".dbActiveConnections";
    public static final String DB_IDLE_CONNECTIONS_MONITOR_ID = WorkItemMonitor.class.getName() + ".dbIdleConnections";

    /* High Priority Active */
    public static final String HIGH_PROIRITY_ACTIVE_MONITOR_ID = "com.serotonin.m2m2.rt.maint.WorkItemMonitor.highPriorityActive";
    private final IntegerMonitor highPriorityActive = 
    		new IntegerMonitor(HIGH_PROIRITY_ACTIVE_MONITOR_ID, new TranslatableMessage("internal.monitor.MONITOR_HIGH_ACTIVE"), this);
    
    /* High Priority Scheduled */
    public static final String HIGH_PRIORITY_SCHEDULED_MONITOR_ID = "com.serotonin.m2m2.rt.maint.WorkItemMonitor.highPriorityScheduled";
    private final IntegerMonitor highPriorityScheduled = 
    		new IntegerMonitor(HIGH_PRIORITY_SCHEDULED_MONITOR_ID, new TranslatableMessage("internal.monitor.MONITOR_HIGH_SCHEDULED"), this);
    /* High Priority Waiting */
    public static final String HIGH_PROIRITY_WAITING_MONITOR_ID = "com.serotonin.m2m2.rt.maint.WorkItemMonitor.highPriorityWaiting";
    private final IntegerMonitor highPriorityWaiting = 
    		new IntegerMonitor(HIGH_PROIRITY_WAITING_MONITOR_ID, new TranslatableMessage("internal.monitor.MONITOR_HIGH_WAITING"), this);

    
    /* Medium Priority Active */
    public static final String MEDIUM_PROIRITY_ACTIVE_MONITOR_ID = "com.serotonin.m2m2.rt.maint.WorkItemMonitor.mediumPriorityActive";
    private final IntegerMonitor mediumPriorityActive = 
    		new IntegerMonitor(MEDIUM_PROIRITY_ACTIVE_MONITOR_ID, new TranslatableMessage("internal.monitor.MONITOR_MEDIUM_ACTIVE"), this);
    /* Medium Priority Waiting */
    public static final String MEDIUM_PROIRITY_WAITING_MONITOR_ID = "com.serotonin.m2m2.rt.maint.WorkItemMonitor.mediumPriorityWaiting";
    private final IntegerMonitor mediumPriorityWaiting = 
    		new IntegerMonitor(MEDIUM_PROIRITY_WAITING_MONITOR_ID, new TranslatableMessage("internal.monitor.MONITOR_MEDIUM_WAITING"), this);

    /* Low Priority Active */
    public static final String LOW_PROIRITY_ACTIVE_MONITOR_ID = "com.serotonin.m2m2.rt.maint.WorkItemMonitor.lowPriorityActive";
    private final IntegerMonitor lowPriorityActive = 
    		new IntegerMonitor(LOW_PROIRITY_ACTIVE_MONITOR_ID, new TranslatableMessage("internal.monitor.MONITOR_LOW_ACTIVE"), this);
    /* Low Priority Waiting */
    public static final String LOW_PROIRITY_WAITING_MONITOR_ID = "com.serotonin.m2m2.rt.maint.WorkItemMonitor.lowPriorityWaiting";
    private final IntegerMonitor lowPriorityWaiting = 
    		new IntegerMonitor(LOW_PROIRITY_WAITING_MONITOR_ID, new TranslatableMessage("internal.monitor.MONITOR_LOW_WAITING"), this);
    
    /* Thread info */
    private final IntegerMonitor maxStackHeight = new IntegerMonitor(MAX_STACK_HEIGHT_MONITOR_ID,
    		new TranslatableMessage("internal.monitor.MONITOR_STACK_HEIGHT"), this);
    private final IntegerMonitor threadCount = new IntegerMonitor(THREAD_COUNT_MONITOR_ID,
    		new TranslatableMessage("internal.monitor.MONITOR_THREAD_COUNT"), this);
    
    /* DB Info */
    private final IntegerMonitor dbActiveConnections = new IntegerMonitor(DB_ACTIVE_CONNECTIONS_MONITOR_ID,
    		new TranslatableMessage("internal.monitor.DB_ACTIVE_CONNECTIONS"), this);
    private final IntegerMonitor dbIdleConnections = new IntegerMonitor(DB_IDLE_CONNECTIONS_MONITOR_ID,
    		new TranslatableMessage("internal.monitor.DB_IDLE_CONNECTIONS"), this);


    private final IntegerMonitor javaMaxMemory = new IntegerMonitor("java.lang.Runtime.maxMemory",
    		new TranslatableMessage("java.monitor.JAVA_MAX_MEMORY"), this);
    private final IntegerMonitor javaUsedMemory = new IntegerMonitor("java.lang.Runtime.usedMemory",
    		new TranslatableMessage("java.monitor.JAVA_USED_MEMORY"), this);
    private final IntegerMonitor javaFreeMemory = new IntegerMonitor("java.lang.Runtime.freeMemory",
    		new TranslatableMessage("java.monitor.JAVA_FREE_MEMORY"), this);
    private final IntegerMonitor javaAvailableProcessors = new IntegerMonitor("java.lang.Runtime.availableProcessors",
    		new TranslatableMessage("java.monitor.JAVA_PROCESSORS"), this);
    
    //System Uptime
    public static final String SYSTEM_UPTIME_MONITOR_ID = "mango.system.uptime";
    		
    private final DoubleMonitor uptime = new DoubleMonitor(SYSTEM_UPTIME_MONITOR_ID, new TranslatableMessage("internal.monitor.SYSTEM_UPTIME"), this);
    
    //User Sessions
    public static final String USER_SESSION_MONITOR_ID = MangoSessionRegistry.class.getCanonicalName() + ".COUNT";
    private final IntegerMonitor userSessions = new IntegerMonitor(USER_SESSION_MONITOR_ID, new TranslatableMessage("internal.monitor.USER_SESSION_COUNT"), this);
    
    private final int mb = 1024*1024;
    
    private boolean running;
    
    private WorkItemMonitor() {
        super(new FixedRateTrigger(TIMEOUT, TIMEOUT), "Work item monitor", "WorkItemMonitor", 0);
        this.running = true;
        
        Common.MONITORED_VALUES.addIfMissingStatMonitor(highPriorityActive);
        Common.MONITORED_VALUES.addIfMissingStatMonitor(highPriorityScheduled);
        Common.MONITORED_VALUES.addIfMissingStatMonitor(highPriorityWaiting);

        Common.MONITORED_VALUES.addIfMissingStatMonitor(mediumPriorityActive);
        Common.MONITORED_VALUES.addIfMissingStatMonitor(mediumPriorityWaiting);

        Common.MONITORED_VALUES.addIfMissingStatMonitor(lowPriorityActive);
        Common.MONITORED_VALUES.addIfMissingStatMonitor(lowPriorityWaiting);
        
        Common.MONITORED_VALUES.addIfMissingStatMonitor(maxStackHeight);
        Common.MONITORED_VALUES.addIfMissingStatMonitor(threadCount);
        Common.MONITORED_VALUES.addIfMissingStatMonitor(dbActiveConnections);
        Common.MONITORED_VALUES.addIfMissingStatMonitor(dbIdleConnections);
        Common.MONITORED_VALUES.addIfMissingStatMonitor(javaFreeMemory);
        Common.MONITORED_VALUES.addIfMissingStatMonitor(javaMaxMemory);
        Common.MONITORED_VALUES.addIfMissingStatMonitor(javaAvailableProcessors);
        Common.MONITORED_VALUES.addIfMissingStatMonitor(uptime);
        
        Common.MONITORED_VALUES.addIfMissingStatMonitor(userSessions);
        
        //Set the available processors, we don't need to poll this
        javaAvailableProcessors.setValue(Runtime.getRuntime().availableProcessors());
                
    }

    @Override
    public void run(long fireTime) {
    	if(!running)
    		return;
        check(fireTime);
    }

    public void check(long fireTime) {
    	
    	if(Common.backgroundProcessing != null){
    		highPriorityActive.setValue(Common.backgroundProcessing.getHighPriorityServiceActiveCount());
    		highPriorityScheduled.setValue(Common.backgroundProcessing.getHighPriorityServiceScheduledTaskCount());
    		highPriorityWaiting.setValue(Common.backgroundProcessing.getHighPriorityServiceQueueSize());
    		
    		mediumPriorityActive.setValue(Common.backgroundProcessing.getMediumPriorityServiceActiveCount());
    		mediumPriorityWaiting.setValue(Common.backgroundProcessing.getMediumPriorityServiceQueueSize());

    		lowPriorityActive.setValue(Common.backgroundProcessing.getLowPriorityServiceActiveCount());
    		lowPriorityWaiting.setValue(Common.backgroundProcessing.getLowPriorityServiceQueueSize());
    	}
    	
    	

        // Check the stack heights
        int max = 0;
        Collection<StackTraceElement[]> stacks = Thread.getAllStackTraces().values();
        int count = stacks.size();
        for (StackTraceElement[] stack : stacks) {
            if (max < stack.length)
                max = stack.length;
            if (stack.length == 0)
                // Don't include inactive threads
                count--;
        }
        threadCount.setValue(count);
        maxStackHeight.setValue(max);

        if(Common.databaseProxy != null){
	        dbActiveConnections.setValue(Common.databaseProxy.getActiveConnections());
	        dbIdleConnections.setValue(Common.databaseProxy.getIdleConnections());
        }
        
        //In MB
        Runtime rt = Runtime.getRuntime();
        javaMaxMemory.setValue((int)(rt.maxMemory()/mb));
        javaUsedMemory.setValue((int)(rt.totalMemory()/mb) -(int)(rt.freeMemory()/mb));   
        javaFreeMemory.setValue(javaMaxMemory.intValue() - javaUsedMemory.intValue());
        
        //Uptime in HRS
        long uptimeMs = fireTime - Common.START_TIME;
        Double uptimeHrs = (double)uptimeMs/3600000.0D;
        BigDecimal bd = new BigDecimal(uptimeHrs);
    	bd = bd.setScale(2, RoundingMode.HALF_UP);
        uptime.setValue(bd.doubleValue());
        
        //Collect Active User Sessions
        
        userSessions.setValue(MangoSecurityConfiguration.getActiveSessionCount());

    }
    
    /* (non-Javadoc)
     * @see com.serotonin.timer.TimerTask#cancel()
     */
    @Override
    public boolean cancel() {
    	this.running = false;
    	return super.cancel();
    }

	/* (non-Javadoc)
	 * @see com.infiniteautomation.mango.monitor.ValueMonitorOwner#reset(com.infiniteautomation.mango.monitor.ValueMonitor)
	 */
	@Override
	public void reset(String id) {
		//No-Op since these values are not counters
	}
}
