/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.util.timeout;

public interface ModelTimeoutClient<T> {
    void scheduleTimeout(T model, long fireTime);
    /**
     * Get the thread name for while the task is executing
     * @return
     */
    String getThreadName();
    
    /**
     * Get the ID for the task for ordering and failure tracking
     * @return
     */
    String getTaskId();
    
    /**
     * Get the queue size for ordered tasks to wait in before being rejected
     * @return
     */
    int getQueueSize();
    
	/**
	 * Is this task able to be queued against tasks with the same ID or should it be run immediately?
	 * @return
	 */
	boolean isQueueable();
}
