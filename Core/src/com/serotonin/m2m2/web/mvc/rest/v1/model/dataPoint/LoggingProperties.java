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
public class LoggingProperties {
	
	public LoggingProperties(LoggingType type){
		this.type = type;
	}
	
	@JsonProperty
	private LoggingType type;

	public LoggingType getType() {
		return type;
	}

	public void setType(LoggingType type) {
		this.type = type;
	}
	
}
