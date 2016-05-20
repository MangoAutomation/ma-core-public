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
     * Get the ID for the task if it is to be ordered,
     * null otherwise
     * @return
     */
    String getTaskId();
    
    /**
     * Get the queue size for ordered tasks to wait in before being rejected
     * @return
     */
    int getQueueSize();
}
