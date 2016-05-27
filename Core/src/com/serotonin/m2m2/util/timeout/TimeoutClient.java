/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.util.timeout;

import com.serotonin.timer.RejectedTaskReason;

public interface TimeoutClient {
	/**
	 * The task logic to run when timeout occurs
	 * @param fireTime
	 */
    void scheduleTimeout(long fireTime);

    /**
     * Get the name for the client's thread while it is running
     * @return
     */
    String getThreadName();
    
    /**
     * Get an ID for Ordered Tasks to keep them in order in the queue and track failures
     * 
     * @return String ID 
     */
    String getTaskId();
    
    /**
     * The size of queue for Ordered Tasks to wait in, when full the tasks get rejected.
     * 
     * @return
     */
    int getQueueSize();
    
    /**
     * Perform any necessary processing when a task is rejected 
     * from its pool due to resource limitations
     * @param reason
     */
    void rejected(RejectedTaskReason reason);

	/**
	 * Is this task able to be queued against tasks with the same ID or should it be run immediately?
	 * @return
	 */
	boolean isQueueable();
}
