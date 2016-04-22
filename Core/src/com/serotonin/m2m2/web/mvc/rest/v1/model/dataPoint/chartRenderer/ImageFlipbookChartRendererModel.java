/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.dataPoint.chartRenderer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.serotonin.m2m2.view.chart.ImageFlipbookRenderer;

/**
 * @author Terry Packer
 *
 */
public class ImageFlipbookChartRendererModel extends BaseChartRendererModel<ImageFlipbookChartRendererModel>{

	@JsonProperty
	private int limit;
	
	public ImageFlipbookChartRendererModel(){ }
	
	public ImageFlipbookChartRendererModel(int limit){
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
		return ImageFlipbookRenderer.getDefinition().getName();
	}

}
