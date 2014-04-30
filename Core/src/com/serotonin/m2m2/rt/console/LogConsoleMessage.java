/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.console;

/**
 * @author Terry Packer
 *
 */
public class LogConsoleMessage implements Comparable<LogConsoleMessage>{

	long timestamp;
	String message; 
		
	public LogConsoleMessage(String message, long timestamp){
		this.message = message;
		this.timestamp = timestamp;
	}

	
	
	/**
	 * @return the timestamp
	 */
	public long getTimestamp() {
		return timestamp;
	}



	/**
	 * @param timestamp the timestamp to set
	 */
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}



	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}



	/**
	 * @param message the message to set
	 */
	public void setMessage(String message) {
		this.message = message;
	}



	/* 
	 * the value 0 if x == y; a value less than 0 if x < y; and a value greater than 0 if x > y
	 * (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(LogConsoleMessage o) {
		return (int) (this.timestamp - o.timestamp);
	}
	
	

}
