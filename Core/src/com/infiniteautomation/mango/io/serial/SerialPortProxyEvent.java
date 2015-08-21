/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.io.serial;

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
