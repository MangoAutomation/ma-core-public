/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.dataPoint;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 * @author Terry Packer
 *
 */
public enum LoggingType {

	ALL,
	INTERVAL,
	NONE,
	ON_CHANGE,
	ON_TS_CHANGE;
	
	/**
	 * Convert from Mango LoggingTypes value to LoggingType
	 * @See com.serotonin.m2m2.vo.DataPointVO.LoggingTypes
	 * @param type
	 * @return
	 */
	public static LoggingType convertTo(int type){
		switch(type){
			case DataPointVO.LoggingTypes.ALL:
				return ALL;
			case DataPointVO.LoggingTypes.INTERVAL:
				return INTERVAL;
			case DataPointVO.LoggingTypes.NONE:
				return NONE;
			case DataPointVO.LoggingTypes.ON_CHANGE:
				return ON_CHANGE;
			case DataPointVO.LoggingTypes.ON_TS_CHANGE:
				return ON_TS_CHANGE;
			default:
				throw new ShouldNeverHappenException("Unknown Logging type value: " + type);
		}
	}
	
	/**
	 * Convert from Logging Type to Mango logging type value
	 * @See com.serotonin.m2m2.vo.DataPointVO.LoggingTypes
	 * @param type
	 * @return
	 */
	public static int convertFrom(LoggingType type){
		switch(type){
			case ALL:
				return DataPointVO.LoggingTypes.ALL;
			case INTERVAL:
				return DataPointVO.LoggingTypes.INTERVAL;
			case NONE:
				return DataPointVO.LoggingTypes.NONE;
			case ON_CHANGE:
				return DataPointVO.LoggingTypes.ON_CHANGE;
			case ON_TS_CHANGE:
				return DataPointVO.LoggingTypes.ON_TS_CHANGE;
			default:
				throw new ShouldNeverHappenException("Unknown Logging type value: " + type);
		}
	}
}
