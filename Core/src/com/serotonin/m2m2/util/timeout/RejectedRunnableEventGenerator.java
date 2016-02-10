/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.util.timeout;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.type.SystemEventType;
import com.serotonin.timer.OrderedTimerTaskWorker;
import com.serotonin.timer.RejectedTaskReason;

/**
 * @author Terry Packer
 *
 */
public class RejectedRunnableEventGenerator implements RejectedExecutionHandler{

	private final Log LOG = LogFactory.getLog(RejectedRunnableEventGenerator.class);
	
	@Override
	public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
		//Types of WorkItem, Runnable and ScheduledRunnable can also be rejected here so we must handle those too for now
		RejectedExecutionException exception = new RejectedExecutionException("Task " + r.toString() +
                " rejected from " +
                e.toString());
		LOG.fatal(exception.getMessage(), exception);

		if(r instanceof OrderedTimerTaskWorker){
			OrderedTimerTaskWorker worker = (OrderedTimerTaskWorker)r;
			worker.reject(new RejectedTaskReason(RejectedTaskReason.POOL_FULL, worker.getExecutionTime(), worker.getTask(), e));
		}else{
			//No known implementation to handle rejections, simply raise event
			SystemEventType.raiseEvent(new SystemEventType(SystemEventType.TYPE_REJECTED_WORK_ITEM), 
            		System.currentTimeMillis(), false, new TranslatableMessage("event.system.rejectedHighPriorityTaskPoolFull", r.toString()));
		}
	}
	
}
