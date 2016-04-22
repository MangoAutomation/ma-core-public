/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.dataPoint.chartRenderer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.serotonin.m2m2.view.chart.TableChartRenderer;

/**
 * @author Terry Packer
 *
 */
public class TableChartRendererModel extends BaseChartRendererModel<TableChartRendererModel>{

	@JsonProperty
	private int limit;
	
	public TableChartRendererModel(){ }
	
	public TableChartRendererModel(int limit){
		this.limit = limit;
	}
	
	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.model.SuperclassModel#getType()
	 */
	@Override
	public String getType() {
		return TableChartRenderer.getDefinition().getName();
	}

}
