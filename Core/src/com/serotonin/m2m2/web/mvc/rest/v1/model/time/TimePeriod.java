/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.time;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Terry Packer
 *
 */
public class TimePeriod {
	
	public TimePeriod(int periods, TimePeriodType type){
		this.periods = periods;
		this.type = type;
	}
	
	@JsonProperty
	private int periods;
	
	@JsonProperty
	private TimePeriodType type;

	public int getPeriods() {
		return periods;
	}

	public void setPeriods(int periods) {
		this.periods = periods;
	}

	public TimePeriodType getType() {
		return type;
	}

	public void setType(TimePeriodType type) {
		this.type = type;
	}

	
	
}
