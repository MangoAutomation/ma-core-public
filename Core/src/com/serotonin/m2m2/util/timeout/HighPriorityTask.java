/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.util.timeout;

import com.serotonin.m2m2.Common;
import com.serotonin.timer.RejectedTaskReason;
import com.serotonin.timer.Task;

/**
 * @author Terry Packer
 *
 */
public abstract class HighPriorityTask extends Task{

	/**
	 * For non-queueing tasks
	 * @param name
	 */
	public HighPriorityTask(String name){
		this(name, null, 0);
	}
	
	/**
	 * For tasks that should be queued
	 * @param name
	 * @param id
	 */
	public HighPriorityTask(String name, String id, int queueSize) {
		super(name, id, queueSize);
	}

	/* (non-Javadoc)
	 * @see com.serotonin.timer.Task#rejected(com.serotonin.timer.RejectedTaskReason)
	 */
	@Override
	public void rejected(RejectedTaskReason reason) {
		Common.rejectionHandler.rejectedHighPriorityTask(reason);
	}
}
