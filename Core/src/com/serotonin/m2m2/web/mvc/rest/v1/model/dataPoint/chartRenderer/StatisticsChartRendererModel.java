/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.dataPoint.chartRenderer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.serotonin.m2m2.view.chart.StatisticsChartRenderer;
import com.serotonin.m2m2.web.mvc.rest.v1.model.dataPoint.TimePeriodModel;

/**
 * @author Terry Packer
 *
 */
public class StatisticsChartRendererModel extends TimePeriodChartRendererModel<StatisticsChartRendererModel>{

	@JsonProperty
	private boolean includeSum;
	
	public StatisticsChartRendererModel(){ }
	
	public StatisticsChartRendererModel(TimePeriodModel timePeriod, boolean includeSum){
		super(timePeriod);
		this.includeSum = includeSum;
	}
	
	public boolean isIncludeSum() {
		return includeSum;
	}

	public void setIncludeSum(boolean includeSum) {
		this.includeSum = includeSum;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.model.SuperclassModel#getType()
	 */
	@Override
	public String getType() {
		return StatisticsChartRenderer.getDefinition().getName();
	}

}
