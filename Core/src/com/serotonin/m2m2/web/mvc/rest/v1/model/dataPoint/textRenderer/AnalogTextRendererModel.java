/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.dataPoint.textRenderer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.serotonin.m2m2.view.text.AnalogRenderer;

/**
 * @author Terry Packer
 *
 */
public class AnalogTextRendererModel extends ConvertingTextRendererModel<AnalogTextRendererModel>{

	@JsonProperty
	private String format;
	@JsonProperty
	private String suffix;
	
	public AnalogTextRendererModel(){ }
	
	/**
	 * @param useUnitAsSuffix
	 * @param unit
	 * @param renderedUnit
	 */
	public AnalogTextRendererModel(boolean useUnitAsSuffix, String unit,
			String renderedUnit, String format, String suffix) {
		super(useUnitAsSuffix, unit, renderedUnit);
		this.format = format;
		this.suffix = suffix;
	}
	
	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public String getSuffix() {
		return suffix;
	}

	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.model.SuperclassModel#getType()
	 */
	@Override
	public String getType() {
		return AnalogRenderer.getDefinition().getName();
	}

}
