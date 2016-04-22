/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.dataPoint.textRenderer;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.serotonin.m2m2.view.text.MultistateRenderer;
import com.serotonin.m2m2.view.text.MultistateValue;

/**
 * @author Terry Packer
 *
 */
public class MultistateTextRendererModel extends BaseTextRendererModel<MultistateTextRendererModel>{

	@JsonProperty
	private List<MultistateValue> multistateValues = new ArrayList<MultistateValue>();
	
	public MultistateTextRendererModel(){ }
	
	public MultistateTextRendererModel(List<MultistateValue> values){
		this.multistateValues = values;
	}
	
	public List<MultistateValue> getMultistateValues() {
		return multistateValues;
	}

	public void setMultistateValues(List<MultistateValue> multistateValues) {
		this.multistateValues = multistateValues;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.model.SuperclassModel#getType()
	 */
	@Override
	public String getType() {
		return MultistateRenderer.getDefinition().getName();
	}

}
