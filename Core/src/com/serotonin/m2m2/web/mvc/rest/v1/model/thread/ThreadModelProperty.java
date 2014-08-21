/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.thread;

/**
 * @author Terry Packer
 *
 */
public enum ThreadModelProperty {
	
	ID,NAME,CPU_TIME,USER_TIME,STATE,PRIORITY,LOCATION;

	/**
	 * @param orderBy
	 * @return
	 */
	public static ThreadModelProperty convert(String orderBy) {
		
		if(orderBy.equalsIgnoreCase("location"))
			return LOCATION;
		else if(orderBy.equalsIgnoreCase("name"))
			return NAME;
		else if(orderBy.equalsIgnoreCase("cpuTime"))
			return CPU_TIME;
		else if(orderBy.equalsIgnoreCase("userTime"))
			return USER_TIME;
		else if(orderBy.equalsIgnoreCase("state"))
			return STATE;
		else if(orderBy.equalsIgnoreCase("priority"))
			return PRIORITY;
		else
			return ID;
		
		
	}
	
	
	
}
