/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.dataSource;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.serotonin.m2m2.web.mvc.rest.v1.model.time.TimePeriod;
import com.serotonin.m2m2.web.mvc.rest.v1.model.time.TimePeriodType;

/**
 * @author Terry Packer
 *
 */
@JsonPropertyOrder({"override"})
public class PurgeSettings {

	/**
	 * 
	 * @param override - Override system defaults
	 * @param periods - number of periods
	 * @param type - Period Type integer
	 */
	public PurgeSettings(boolean override, int periods, int type){
		this.override = override;
		this.frequency = new TimePeriod(periods ,TimePeriodType.convertTo(type));
	}
	
	public PurgeSettings(){
		
	}
	
	@JsonProperty("override")
	private boolean override;
	
	@JsonProperty("frequency")
	private TimePeriod frequency;

	public boolean isOverride() {
		return override;
	}

	public void setOverride(boolean override) {
		this.override = override;
	}

	public TimePeriod getFrequency() {
		return frequency;
	}

	public void setFrequency(TimePeriod frequency) {
		this.frequency = frequency;
	}
	

	
}
