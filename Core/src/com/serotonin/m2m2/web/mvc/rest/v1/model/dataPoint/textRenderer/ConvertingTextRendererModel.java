/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.dataPoint.textRenderer;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Terry Packer
 *
 */
public abstract class ConvertingTextRendererModel<T> extends BaseTextRendererModel<T>{

	@JsonProperty
	private boolean useUnitAsSuffix;
	@JsonProperty
	private String unit;
	@JsonProperty
	private String renderedUnit;
	
	public ConvertingTextRendererModel(){ }
	
	/**
	 * @param useUnitAsSuffix
	 * @param unit
	 * @param renderedUnit
	 */
	public ConvertingTextRendererModel(boolean useUnitAsSuffix, String unit,
			String renderedUnit) {
		super();
		this.useUnitAsSuffix = useUnitAsSuffix;
		this.unit = unit;
		this.renderedUnit = renderedUnit;
	}
	public boolean isUseUnitAsSuffix() {
		return useUnitAsSuffix;
	}
	public void setUseUnitAsSuffix(boolean useUnitAsSuffix) {
		this.useUnitAsSuffix = useUnitAsSuffix;
	}
	public String getUnit() {
		return unit;
	}
	public void setUnit(String unit) {
		this.unit = unit;
	}
	public String getRenderedUnit() {
		return renderedUnit;
	}
	public void setRenderedUnit(String renderedUnit) {
		this.renderedUnit = renderedUnit;
	}
	
}
