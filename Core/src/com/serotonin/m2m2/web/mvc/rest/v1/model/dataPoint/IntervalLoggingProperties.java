/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.dataPoint;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Terry Packer
 *
 */
public class IntervalLoggingProperties extends LoggingProperties{

	@JsonProperty
	private IntervalLoggingType intervalType;
	
	/**
	 * @param type
	 */
	public IntervalLoggingProperties(LoggingType type, IntervalLoggingType intervalType) {
		super(type);
		this.intervalType = intervalType;
	}

	public IntervalLoggingType getIntervalType() {
		return intervalType;
	}

	public void setIntervalType(IntervalLoggingType intervalType) {
		this.intervalType = intervalType;
	}

	
	
}
