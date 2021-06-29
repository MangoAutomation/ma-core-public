/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.io.serial;

/**
 * @author Terry Packer
 *
 */
public class SerialPortProxyEvent {
	
	private long creationTime;
	private long timeExecuted;
	private int bytesRead;
	
	public SerialPortProxyEvent(long time){
		this.creationTime = time;
	}
	
	public SerialPortProxyEvent(long time, int bytesRead){
        this.creationTime = time;
        this.bytesRead = bytesRead;
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
    
	public int getBytesRead() {
        return bytesRead;
    }
}
