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
public enum IntervalLoggingType {
	AVERAGE,
	INSTANT,
	MAXIMUM,
	MINIMUM;
	
	/**
	 * Convert from the Mango int value to an Interval Logging type 
	 * @param type
	 * @return
	 */
	public static IntervalLoggingType convertTo(int type){
		switch (type){
			case DataPointVO.IntervalLoggingTypes.AVERAGE:
				return AVERAGE;
			case DataPointVO.IntervalLoggingTypes.INSTANT:
				return INSTANT;
			case DataPointVO.IntervalLoggingTypes.MAXIMUM:
				return MAXIMUM;
			case DataPointVO.IntervalLoggingTypes.MINIMUM:
				return MINIMUM;
			default:
				throw new ShouldNeverHappenException("Unknown Interval Logging Type: " + type);
		}
	}
	
	/**
	 * Convert from an Interval Logging Type to the Mango int value
	 * @param type
	 * @return
	 */
	public static int convertFrom(IntervalLoggingType type){
		switch (type){
		case AVERAGE:
			return DataPointVO.IntervalLoggingTypes.AVERAGE;
		case INSTANT:
			return DataPointVO.IntervalLoggingTypes.INSTANT;
		case MAXIMUM:
			return DataPointVO.IntervalLoggingTypes.MAXIMUM;
		case MINIMUM:
			return DataPointVO.IntervalLoggingTypes.MINIMUM;
		default:
			throw new ShouldNeverHappenException("Unknown Interval Logging Type: " + type);
	}
	}
	
}
