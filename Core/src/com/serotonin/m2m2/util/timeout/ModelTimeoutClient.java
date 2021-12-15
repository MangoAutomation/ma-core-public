/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.util.timeout;

public interface ModelTimeoutClient<T> {
    void scheduleTimeout(T model, long fireTime);
    /**
     * Get the thread name for while the task is executing
     */
    String getThreadName();
    
    /**
     * Get the ID for the task for ordering and failure tracking,
     * Task ID of null means no order/queue and run in parallel
     */
    String getTaskId();
    
    /**
     * Get the queue size for ordered tasks to wait in before being rejected
     */
    int getQueueSize();

}
