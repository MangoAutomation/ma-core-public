/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.dataPoint.chartRenderer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.serotonin.m2m2.web.mvc.rest.v1.model.dataPoint.TimePeriodModel;

/**
 * @author Terry Packer
 *
 */
public abstract class TimePeriodChartRendererModel<T> extends BaseChartRendererModel<T>{

	
	@JsonProperty 
	private TimePeriodModel timePeriod;
	
	public TimePeriodChartRendererModel(){ }
	
	public TimePeriodChartRendererModel(TimePeriodModel timePeriod){
		this.timePeriod = timePeriod;
	}

	public TimePeriodModel getTimePeriod() {
		return timePeriod;
	}

	public void setTimePeriod(TimePeriodModel timePeriod) {
		this.timePeriod = timePeriod;
	}
	
}
