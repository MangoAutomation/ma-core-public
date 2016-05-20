/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.io.serial;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.m2m2.Common;

/**
 * @author Terry Packer
 *
 */
public class SerialPortProxyEventTask extends Thread {
	private final Log LOG = LogFactory.getLog(SerialPortProxyEventTask.class);
	
	private SerialPortProxyEventListener listener;
	private SerialPortProxyEvent event;
	private long creationTime;
	private SerialPortProxyEventCompleteListener completeListener;
	
	
	public SerialPortProxyEventTask(SerialPortProxyEventListener listener, SerialPortProxyEvent event, SerialPortProxyEventCompleteListener completeListener){
		this.creationTime = Common.backgroundProcessing.currentTimeMillis();
		this.listener = listener;
		this.event = event;
		this.completeListener = completeListener;
	}

	@Override
	public void run() {
		
		try{
			if(LOG.isDebugEnabled())
				LOG.debug("Running event created at: " + this.event.getCreationTime());
			
			this.event.setTimeExecuted(Common.backgroundProcessing.currentTimeMillis());
			listener.serialEvent(this.event);
		}catch(Exception e){
			LOG.error(e);
		}finally{
			//I'm done here
			this.completeListener.eventComplete(Common.backgroundProcessing.currentTimeMillis(), this);
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
