/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.util.timeout;

import com.serotonin.m2m2.Common;
import com.serotonin.timer.RejectedTaskReason;
import com.serotonin.timer.Task;

/**
 * @author Terry Packer
 *
 */
public abstract class HighPriorityTask extends Task {

	/**
	 * For non-queueing tasks
     */
	public HighPriorityTask(String name){
		super(name);
	}
	
	/**
	 * For tasks that should be queued
	 * @param id - Non null String identifier for rejection tracking and ordering
     */
	public HighPriorityTask(String name, String id, int queueSize) {
		super(name, id, queueSize);
	}

	/* (non-Javadoc)
	 * @see com.serotonin.timer.Task#rejected(com.serotonin.timer.RejectedTaskReason)
	 */
	@Override
	public void rejected(RejectedTaskReason reason) {
		Common.backgroundProcessing.rejectedHighPriorityTask(reason);
	}
}
