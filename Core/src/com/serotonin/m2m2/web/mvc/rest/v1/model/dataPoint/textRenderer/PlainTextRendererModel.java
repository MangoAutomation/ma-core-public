/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.dataPoint.textRenderer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.serotonin.m2m2.view.text.PlainRenderer;

/**
 * @author Terry Packer
 *
 */
public class PlainTextRendererModel extends ConvertingTextRendererModel<PlainTextRendererModel>{

	@JsonProperty
	private String suffix;
	
	public PlainTextRendererModel(){ }
	
	/**
	 * @param useUnitAsSuffix
	 * @param unit
	 * @param renderedUnit
	 */
	public PlainTextRendererModel(boolean useUnitAsSuffix, String unit,
			String renderedUnit, String suffix) {
		super(useUnitAsSuffix, unit, renderedUnit);
		this.suffix = suffix;
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
		return PlainRenderer.getDefinition().getName();
	}

}
