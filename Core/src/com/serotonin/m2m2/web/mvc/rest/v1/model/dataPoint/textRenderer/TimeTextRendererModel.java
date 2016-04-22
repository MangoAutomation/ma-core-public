/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.dataPoint.textRenderer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.serotonin.m2m2.view.text.TimeRenderer;

/**
 * @author Terry Packer
 *
 */
public class TimeTextRendererModel extends BaseTextRendererModel<TimeTextRendererModel>{

    @JsonProperty
    private String format;
    @JsonProperty 
    private int conversionExponent;
    
    public TimeTextRendererModel(){ }
	
	public TimeTextRendererModel(String format, int conversionExponent) {
		super();
		this.format = format;
		this.conversionExponent = conversionExponent;
	}
	
	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public int getConversionExponent() {
		return conversionExponent;
	}

	public void setConversionExponent(int conversionExponent) {
		this.conversionExponent = conversionExponent;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.model.SuperclassModel#getType()
	 */
	@Override
	public String getType() {
		return TimeRenderer.getDefinition().getName();
	}

}
