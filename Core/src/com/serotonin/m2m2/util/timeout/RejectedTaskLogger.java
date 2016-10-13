/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.util.timeout;

import java.lang.reflect.Field;
import java.util.concurrent.RejectedExecutionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.timer.RejectedTaskReason;

/**
 * Simple class to handle task rejections by logging them.  
 * 
 * Override as necessary to perform additional action(s).
 * 
 * @author Terry Packer
 *
 */
public class RejectedTaskLogger implements RejectedTaskHandler{

	private final Log LOG = LogFactory.getLog(RejectedTaskLogger.class);
	
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
		} //TODO Other types?
		
		
		RejectedExecutionException exception = new RejectedExecutionException("Task " + taskName +
                " rejected from " + reason.getExecutor().toString());
		LOG.fatal(exception.getMessage(), exception);
	}
	
}
