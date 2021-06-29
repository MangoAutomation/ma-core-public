/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.util.timeout;

import com.serotonin.m2m2.Common;
import com.serotonin.timer.RejectedTaskReason;

/**
 * Timeout Clients run and can be ordered into a queue by Task ID, 
 * if the Task ID is null then the tasks will be run in the Thread Pool 
 * and potentially in parallel
 * 
 * @author Terry Packer
 */
public abstract class TimeoutClient {
	/**
	 * The task logic to run when timeout occurs
	 * @param fireTime
	 */
    abstract public void scheduleTimeout(long fireTime);

    /**
     * Get the name for the client's thread while it is running
     * @return
     */
    abstract public String getThreadName();
    
    /**
     * Get an ID for Ordered Tasks to keep them in order in the queue and track failures,
     * null IDs are used to indicate no order and the tasks can be run in parallel
     * 
     * @return String ID 
     */
    public String getTaskId(){
    	return null;
    }
    
    /**
     * The size of queue for Ordered Tasks to wait in, when full the tasks get rejected.
     * 
     * @return
     */
    public int getQueueSize(){
    	return Common.defaultTaskQueueSize;
    }
    
    /**
     * Perform any necessary processing when a task is rejected 
     * from its pool due to resource limitations
     * @param reason
     */
    public void rejected(RejectedTaskReason reason){
    	Common.backgroundProcessing.rejectedHighPriorityTask(reason);
    }

}
