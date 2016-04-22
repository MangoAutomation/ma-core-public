/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.dataPoint.chartRenderer;

import com.serotonin.m2m2.view.chart.ImageChartRenderer;
import com.serotonin.m2m2.web.mvc.rest.v1.model.dataPoint.TimePeriodModel;

/**
 * @author Terry Packer
 *
 */
public class ImageChartRendererModel extends TimePeriodChartRendererModel<ImageChartRendererModel>{

	public ImageChartRendererModel(){ }
	
	public ImageChartRendererModel(TimePeriodModel timePeriod){
		super(timePeriod);
	}
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.model.SuperclassModel#getType()
	 */
	@Override
	public String getType() {
		return ImageChartRenderer.getDefinition().getName();
	}

}
