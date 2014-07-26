/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.permissions;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.vo.permission.DataPointAccess;
import com.serotonin.m2m2.web.mvc.rest.v1.model.events.AlarmLevel;

/**
 * @author Terry Packer
 *
 */
public enum PermissionEnum {
	
	READ,
	SET;
	
	
	/**
	 * Convert Mango's 
	 * @See com.serotonin.m2m2.vo.permission.DataPointAccess
	 *  to the Enum Type PermissionEnum
	 * 
	 * 
	 * @param level
	 * @return
	 */
	public static PermissionEnum convertTo(int level) {
		switch(level){
		case DataPointAccess.READ:
			return READ;
		case DataPointAccess.SET:
			return SET;
		default:
			throw new ShouldNeverHappenException("No PermissionEnum for value :" + level);
		}
	}
	
	/**
	 * Convert this enum into  
	 * @See com.serotonin.m2m2.vo.permission.DataPointAccess
	 * 
	 * @param level
	 * @return
	 */
	public static int convertFrom(PermissionEnum level) {
		switch(level){
		case READ:
			return DataPointAccess.READ;
		case SET:
			return DataPointAccess.SET; 
		default:
			throw new ShouldNeverHappenException("No DataPointAccess exists for value :" + level);
		}
	}
	
	
}
