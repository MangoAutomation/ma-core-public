/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.events;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.rt.event.AlarmLevels;

/**
 * @author Terry Packer
 *
 */
public enum AlarmLevel {

	NONE,
	INFORMATION,
	URGENT,
	CRITICAL,
	LIFE_SAFETY,
	DO_NOT_LOG;
	
	/**
	 * Convert Mango's 
	 * @See Ccom.serotonin.m2m2.rt.event.AlarmLevels
	 *  to the Enum Type AlarmLevel
	 * 
	 * 
	 * @param level
	 * @return
	 */
	public static AlarmLevel convertTo(int level) {
		switch(level){
		case AlarmLevels.NONE:
			return NONE;
		case AlarmLevels.INFORMATION:
			return INFORMATION; 
		case AlarmLevels.URGENT:
			return URGENT;
		case AlarmLevels.CRITICAL:
			return CRITICAL;
		case AlarmLevels.LIFE_SAFETY:
			return LIFE_SAFETY;
		case AlarmLevels.DO_NOT_LOG:
			return DO_NOT_LOG;
		default:
			throw new ShouldNeverHappenException("No AlarmLevel exists for value :" + level);
		}
	}
	
	/**
	 * Convert this enum into  
	 * @See Common.TimePeriods 
	 * 
	 * @param updatePeriodType
	 * @return
	 */
	public static int convertFrom(AlarmLevel level) {
		switch(level){
		case NONE:
			return AlarmLevels.NONE;
		case INFORMATION:
			return AlarmLevels.INFORMATION; 
		case URGENT:
			return AlarmLevels.URGENT;
		case  CRITICAL:
			return AlarmLevels.CRITICAL;
		case  LIFE_SAFETY:
			return AlarmLevels.LIFE_SAFETY;
		case DO_NOT_LOG:
			return AlarmLevels.DO_NOT_LOG;
		default:
			throw new ShouldNeverHappenException("No AlarmLevel exists for value :" + level);
		}
	}
	
}
