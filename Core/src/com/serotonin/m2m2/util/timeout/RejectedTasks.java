/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.util.timeout;

import com.serotonin.timer.RejectedTaskReason;

/**
 * 
 * Store for rejected tasks from an Execution Handler where they can be retrieved 
 * for later analysis.
 * 
 * Designed for performance in collecting rejection information and minimizing 
 * system load when tasks are being rejected.
 * 
 * @author Terry Packer
 *
 */
public class RejectedTasks implements RejectionHandler{
	
	/**
	 * ms to wait before logging another rejection for a given task
	 */
	int logPeriod;
	//Map to hold task ID and its rejections with a timestamp to ensure proper logging
	
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.util.timeout.RejectedTaskHandler#rejected(com.serotonin.timer.RejectedTaskReason)
	 */
	@Override
	public void rejected(RejectedTaskReason reason) {
		
	}

	
	
	
}
