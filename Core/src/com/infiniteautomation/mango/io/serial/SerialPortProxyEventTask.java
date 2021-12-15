/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.io.serial;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.m2m2.Common;

/**
 * @author Terry Packer
 *
 */
public class SerialPortProxyEventTask {
	private final Logger LOG = LoggerFactory.getLogger(SerialPortProxyEventTask.class);
	
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
			LOG.error("An error occurred", e);
		}
	}
	
	/**
	 * Get the time the task was created
     */
	public long getEventCreationTime(){
		return this.creationTime;
	}

}
