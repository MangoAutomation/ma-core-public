/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.dataPoint.textRenderer;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.serotonin.m2m2.view.text.RangeRenderer;
import com.serotonin.m2m2.view.text.RangeValue;

/**
 * @author Terry Packer
 *
 */
public class RangeTextRendererModel extends ConvertingTextRendererModel<RangeTextRendererModel>{

	@JsonProperty
	private String format;
	@JsonProperty
	private List<RangeValue> rangeValues;
	
	public RangeTextRendererModel(){ }
	
	/**
	 * @param useUnitAsSuffix
	 * @param unit
	 * @param renderedUnit
	 */
	public RangeTextRendererModel(boolean useUnitAsSuffix, String unit,
			String renderedUnit, String format, List<RangeValue> rangeValues) {
		super(useUnitAsSuffix, unit, renderedUnit);
		this.format = format;
		this.rangeValues = rangeValues;
	}
	
	public List<RangeValue> getRangeValues() {
		return rangeValues;
	}

	public void setRangeValues(List<RangeValue> rangeValues) {
		this.rangeValues = rangeValues;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.model.SuperclassModel#getType()
	 */
	@Override
	public String getType() {
		return RangeRenderer.getDefinition().getName();
	}

}
