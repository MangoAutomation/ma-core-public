/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.util.timeout;

import com.serotonin.m2m2.Common;
import com.serotonin.timer.RejectedTaskReason;
import com.serotonin.timer.TimerTask;
import com.serotonin.timer.TimerTrigger;

/**
 * Class to wrap Timer Tasks in a container that will 
 * handle their rejection in a common way.  These tasks 
 * are all run in the High Priority Pool.
 * 
 * @author Terry Packer
 *
 */
public abstract class RejectableTimerTask extends TimerTask{
	
	/**
	 * 
	 * @param trigger
	 * @param name
	 * @param id - Unique String ID for task to build queue and provide Status Information 
	 * @param queueSize
	 */
	public RejectableTimerTask(TimerTrigger trigger, String name, String id, int queueSize) {
		super(trigger, name, id, queueSize);
	}

	/* (non-Javadoc)
	 * @see com.serotonin.timer.TimerTask#rejected(com.serotonin.timer.RejectedTaskReason)
	 */
	@Override
	public void rejected(RejectedTaskReason reason) {
		Common.rejectionHandler.rejectedHighPriorityTask(reason);
	}

}
