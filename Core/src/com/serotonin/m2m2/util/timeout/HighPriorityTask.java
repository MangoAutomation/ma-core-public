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
	 * 
	 * @param name
	 * @param id - Non null String identifier for rejection tracking and ordering
	 * @param queueSize
	 * @param queueable
	 */
	public HighPriorityTask(String name, String id, int queueSize, boolean queueable) {
		super(name, id, queueSize, queueable);
	}

	/* (non-Javadoc)
	 * @see com.serotonin.timer.Task#rejected(com.serotonin.timer.RejectedTaskReason)
	 */
	@Override
	public void rejected(RejectedTaskReason reason) {
		Common.highPriorityRejectionHandler.rejected(reason);
	}
}
