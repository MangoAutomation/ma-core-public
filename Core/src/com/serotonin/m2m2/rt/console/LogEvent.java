/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.console;

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

}
