/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.io.serial;

/**
 * @author Terry Packer
 * 
 */
public class SerialPortProxyEvent {
	
	private long creationTime;
	private long timeExecuted;
	
	public SerialPortProxyEvent(long time){
		this.creationTime = time;
	}
	
	public long getCreationTime(){
		return this.creationTime;
	}

	public void setTimeExecuted(long time) {
		this.timeExecuted = time;
	}
	public long getTimeExecuted(){
		return this.timeExecuted;
	}
}
