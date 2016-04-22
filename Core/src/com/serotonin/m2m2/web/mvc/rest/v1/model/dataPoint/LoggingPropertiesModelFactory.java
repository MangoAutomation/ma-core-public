/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.dataPoint;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 * @author Terry Packer
 *
 */
public class LoggingPropertiesModelFactory {

	public static LoggingPropertiesModel createModel(DataPointVO vo){

		return new LoggingPropertiesModel(
				DataPointVO.LOGGING_TYPE_CODES.getCode(vo.getLoggingType()),
				DataPointVO.INTERVAL_LOGGING_TYPE_CODES.getCode(vo.getIntervalLoggingType()),
				new TimePeriodModel(vo.getIntervalLoggingPeriod(), vo.getIntervalLoggingPeriodType()),
				vo.getTolerance(),
				vo.isDiscardExtremeValues(),
				vo.getDiscardLowLimit(),
				vo.getDiscardHighLimit(),
				vo.isOverrideIntervalLoggingSamples(),
				vo.getIntervalLoggingSampleWindowSize(),
				vo.getDefaultCacheSize()
				);
	}

	/**
	 * Update the VO with the new properties
	 * @param data
	 * @param loggingProperties
	 */
	public static void updateDataPoint(DataPointVO vo,
			LoggingPropertiesModel loggingProperties) {
		
		vo.setLoggingType(DataPointVO.LOGGING_TYPE_CODES.getId(loggingProperties.getLoggingType()));
		vo.setIntervalLoggingType(DataPointVO.INTERVAL_LOGGING_TYPE_CODES.getId(loggingProperties.getIntervalLoggingType()));
		vo.setIntervalLoggingPeriod(loggingProperties.getIntervalLoggingPeriod().getPeriods());
		vo.setIntervalLoggingPeriodType(Common.TIME_PERIOD_CODES.getId(loggingProperties.getIntervalLoggingPeriod().getPeriodType()));
		vo.setTolerance(loggingProperties.getTolerance());
		vo.setDiscardExtremeValues(loggingProperties.isDiscardExtremeValues());
		vo.setDiscardLowLimit(loggingProperties.getDiscardLowLimit());
		vo.setDiscardHighLimit(loggingProperties.getDiscardHighLimit());
		vo.setOverrideIntervalLoggingSamples(loggingProperties.isOverrideIntervalLoggingSamples());
		vo.setIntervalLoggingSampleWindowSize(loggingProperties.getIntervalLoggingSampleWindowSize());
		vo.setDefaultCacheSize(loggingProperties.getDefaultCacheSize());
	}
	
}
