/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.io.serial;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.m2m2.Common;

/**
 * @author Terry Packer
 *
 */
public class SerialPortProxyEventTask {
	private final Log LOG = LogFactory.getLog(SerialPortProxyEventTask.class);
	
	private SerialPortProxyEventListener listener;
	private SerialPortProxyEvent event;
	private long creationTime;
	
	
	public SerialPortProxyEventTask(SerialPortProxyEventListener listener, SerialPortProxyEvent event){
		this.creationTime = Common.timer.currentTimeMillis();
		this.listener = listener;
		this.event = event;
	}

	/**
	 * Notify the listener
	 */
	public void run() {
		try{
			if(LOG.isDebugEnabled())
				LOG.debug("Running event created at: " + this.event.getCreationTime());
			
			this.event.setTimeExecuted(Common.timer.currentTimeMillis());
			listener.serialEvent(this.event);
		}catch(Exception e){
			LOG.error(e);
		}
	}
	
	/**
	 * Get the time the task was created
	 * @return
	 */
	public long getEventCreationTime(){
		return this.creationTime;
	}

}
