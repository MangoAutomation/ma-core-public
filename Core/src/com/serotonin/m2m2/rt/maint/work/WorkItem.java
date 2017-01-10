/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.maint.work;

/**
 * @author Matthew Lohbihler
 * 
 */
public interface WorkItem {
	
    /**
     * Uses a thread pool to immediately execute a process.
     */
    int PRIORITY_HIGH = 1;

    /**
     * Uses a single thread to execute processes sequentially. Assumes that processes will complete in a reasonable time
     * so that other processes do not have to wait long.
     */
    int PRIORITY_MEDIUM = 2;

    /**
     * Uses a single thread to execute processes sequentially. Assumes that processes can wait indefinately to run
     * without consequence.
     */
    int PRIORITY_LOW = 3;

    void execute();

    /**
     * Get our priority level
     * @return
     * 
     */
    int getPriority();
    
    /**
     * Return a one line useful description of what we are doing
     * @return
     */
    public String getDescription();
    
}
