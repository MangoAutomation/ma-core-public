/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.maint.work;

import com.serotonin.timer.RejectedTaskReason;

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
     * Uses a single thread to execute processes sequentially. Assumes that processes can wait indefinitely to run
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

    /**
     * Get the id to bundle similar tasks in an ordered queue
     * returning null indicates no ordering necessary
     * @return
     */
    default public String getTaskId() {
        return null;
    }

    /**
     * How many tasks can be scheduled and waiting to run in the Ordered Timer
     * 0 means that only 1 item can be run all others will be discarded if the task is Ordered
     * @return
     */
    default public int getQueueSize() {
        return 0;
    }


    /**
     * If any special handling needs to be done about the rejection, handle it in this method.
     * General task failure tracking is already handled by the core.
     *
     * @param reason
     */
    public void rejected(RejectedTaskReason reason);

}
