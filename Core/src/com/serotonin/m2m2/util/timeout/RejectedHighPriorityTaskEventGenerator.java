/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.util.timeout;

import java.lang.reflect.Field;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.type.SystemEventType;
import com.serotonin.timer.RejectedTaskReason;

/**
 * 
 * Class to handle rejected tasks from the RealTimeTimer
 * 
 * @author Terry Packer
 *
 */
public class RejectedHighPriorityTaskEventGenerator implements RejectedTaskHandler{

	private final Log LOG = LogFactory.getLog(RejectedHighPriorityTaskEventGenerator.class);
	
	@Override
	public void rejected(RejectedTaskReason reason){
		String taskName;
		if(reason.getTask() instanceof TimeoutTask){
			try {
				TimeoutTask task = (TimeoutTask)reason.getTask();
				Field f;
				f = task.getClass().getDeclaredField("client");
				f.setAccessible(true);
				Class<?> c = f.get(task).getClass();
				taskName = c.getName();
			} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
				taskName = reason.getTask().toString();
			}

		}else{
			taskName = reason.getTask().toString();
		}
		
		
		LOG.fatal("High priority task: " + taskName + " rejected because " + reason.getDescription());
		
    	//Raise Event that Task Was Rejected
    	switch(reason.getCode()){
    	case RejectedTaskReason.CURRENTLY_RUNNING:
    		SystemEventType.raiseEvent(new SystemEventType(SystemEventType.TYPE_REJECTED_WORK_ITEM), 
            		reason.getScheduledExecutionTime(), false, new TranslatableMessage("event.system.rejectedHighPriorityTaskAlreadyRunning", taskName));
    		break;
    	case RejectedTaskReason.POOL_FULL:
    		SystemEventType.raiseEvent(new SystemEventType(SystemEventType.TYPE_REJECTED_WORK_ITEM), 
            		reason.getScheduledExecutionTime(), false, new TranslatableMessage("event.system.rejectedHighPriorityTaskPoolFull", taskName));
    		break;
    	case RejectedTaskReason.TASK_QUEUE_FULL:
    		SystemEventType.raiseEvent(new SystemEventType(SystemEventType.TYPE_REJECTED_WORK_ITEM), 
            		reason.getScheduledExecutionTime(), false, new TranslatableMessage("event.system.rejectedHighPriorityTaskQueueFull", taskName));
    		break;
    	default:
    		LOG.error("Task rejected for unknownReason: " + reason.getCode() + " - " + reason.getDescription());
    	}
	
	}

}
