/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2;

import static org.junit.Assert.fail;

import java.util.concurrent.ThreadPoolExecutor;

import com.serotonin.m2m2.util.timeout.TaskRejectionHandler;
import com.serotonin.timer.RejectedTaskReason;

/**
 *
 * @author Terry Packer
 */
public class AssertTaskRejectionHandler extends TaskRejectionHandler {

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.util.timeout.TaskRejectionHandler#rejectedTask(com.serotonin.timer.RejectedTaskReason)
     */
    @Override
    public void rejectedTask(RejectedTaskReason reason) {
        fail("Rejected task: " + reason);
    }
    
    /* (non-Javadoc)
     * @see java.util.concurrent.RejectedExecutionHandler#rejectedExecution(java.lang.Runnable, java.util.concurrent.ThreadPoolExecutor)
     */
    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        fail("Rejected task " + r.toString());
    }

}
