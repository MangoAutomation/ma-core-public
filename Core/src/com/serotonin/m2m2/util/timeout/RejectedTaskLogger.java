/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.util.timeout;

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
		
		RejectedExecutionException exception = new RejectedExecutionException("Task " + reason.getTask().toString() +
                " rejected from " +
                reason.getExecutor().toString());
		LOG.fatal(exception.getMessage(), exception);
	}
	
}
