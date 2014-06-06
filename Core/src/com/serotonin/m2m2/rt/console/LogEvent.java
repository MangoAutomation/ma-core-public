/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.console;

import org.apache.log4j.Level;

import com.serotonin.m2m2.DeltamationCommon;
import com.serotonin.m2m2.view.stats.ITime;

/**
 * 
 * Nice Clean Way to display Log Events in the UI
 * 
 * @author Terry Packer
 *
 */
public class LogEvent implements ITime{

	private long time;
	private int level;
	private String message;
	private String locationInfo;
	
	public LogEvent(){
		
	}
	
	/**
	 * @param timestamp
	 * @param level
	 * @param message
	 * @param locationInfo
	 */
	public LogEvent(long time, int level, String message,
			String locationInfo) {
		super();
		
		this.time = time;
		this.level = level;
		this.message = message;
		this.locationInfo = locationInfo;
	}

	/**
	 * @return the level
	 */
	public int getLevel() {
		return level;
	}
	/**
	 * @param level the level to set
	 */
	public void setLevel(int level) {
		this.level = level;
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
	/**
	 * @return the locationInfo
	 */
	public String getLocationInfo() {
		return locationInfo;
	}
	/**
	 * @param locationInfo the locationInfo to set
	 */
	public void setLocationInfo(String locationInfo) {
		this.locationInfo = locationInfo;
	}
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.view.stats.ITime#getTime()
	 */
	@Override
	public long getTime() {
		return this.time;
	}
	
	public void setTime(long time){
		this.time = time;
	}

	public String getPrettyTime(){
		return DeltamationCommon.formatDate(this.time);
	}
	public void setPrettyTime(String s){
		//no-op
	}

	public String getPrettyLevel(){
		switch(this.level){
		case Level.TRACE_INT:
			return "<font color='white'>Trace</font>";
		case Level.DEBUG_INT:
			return "<font color='green'>Debug</font>";
		case Level.INFO_INT:
			return "<font color='blue'>Info</font>";
		case Level.WARN_INT:
			return "<font color='orange'>Warn</font>";
		case Level.ERROR_INT:
			return "<font color='red'>Error</font>";
		case Level.FATAL_INT:
			return "<font color='dark-red'>Fatal</font>";
		default:
			return "Unknown";
		}
	}
	
	public void setPrettyLevel(String level){
		//no-op
	}
	
}
