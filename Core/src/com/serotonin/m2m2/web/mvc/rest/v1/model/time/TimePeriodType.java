/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.time;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;


/**
 * @author Terry Packer
 *
 */
public enum TimePeriodType {

	MILLISECONDS,
	SECONDS,
	MINUTES,
	HOURS,
	DAYS,
	WEEKS,
	MONTHS,
	YEARS;

	/**
	 * Convert Mango's 
	 * @See Common.TimePeriods to the Enum Type TimePeriodType
	 * 
	 * 
	 * @param updatePeriodType
	 * @return
	 */
	public static TimePeriodType convertTo(int updatePeriodType) {
		switch(updatePeriodType){
		case Common.TimePeriods.MILLISECONDS:
			return MILLISECONDS;
		case Common.TimePeriods.SECONDS:
			return SECONDS; 
		case Common.TimePeriods.MINUTES:
			return MINUTES;
		case Common.TimePeriods.HOURS:
			return HOURS;
		case Common.TimePeriods.DAYS:
			return DAYS;
		case Common.TimePeriods.WEEKS:
			return WEEKS;
		case Common.TimePeriods.MONTHS:
			return MONTHS;
		case Common.TimePeriods.YEARS:
			return YEARS;
		default:
			throw new ShouldNeverHappenException("No Time Period Type exists for value :" + updatePeriodType);
		}
	}
	
	/**
	 * Convert this enum into  
	 * @See Common.TimePeriods 
	 * 
	 * @param updatePeriodType
	 * @return
	 */
	public static int convertFrom(TimePeriodType type) {
		switch(type){
		case MILLISECONDS:
			return Common.TimePeriods.MILLISECONDS;
		case SECONDS:
			return Common.TimePeriods.SECONDS; 
		case MINUTES:
			return Common.TimePeriods.MINUTES;
		case HOURS:
			return Common.TimePeriods.HOURS;
		case DAYS:
			return Common.TimePeriods.DAYS;
		case WEEKS:
			return Common.TimePeriods.WEEKS;
		case MONTHS:
			return Common.TimePeriods.MONTHS;
		case YEARS:
			return Common.TimePeriods.YEARS;
		default:
			throw new ShouldNeverHappenException("No Common.TimePeriods value exists for type :" + type);
		}
	}
}
