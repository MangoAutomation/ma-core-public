/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.dataPoint.textRenderer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.serotonin.m2m2.view.text.BinaryTextRenderer;

/**
 * @author Terry Packer
 *
 */
public class BinaryTextRendererModel extends BaseTextRendererModel<BinaryTextRendererModel>{

    @JsonProperty
    private String zeroLabel;
    @JsonProperty
    private String zeroColour;
    @JsonProperty
    private String oneLabel;
    @JsonProperty
    private String oneColour;
    
    public BinaryTextRendererModel(){ }
	
	/**
	 * @param zeroLabel
	 * @param zeroColour
	 * @param oneLabel
	 * @param oneColour
	 */
	public BinaryTextRendererModel(String zeroLabel, String zeroColour,
			String oneLabel, String oneColour) {
		super();
		this.zeroLabel = zeroLabel;
		this.zeroColour = zeroColour;
		this.oneLabel = oneLabel;
		this.oneColour = oneColour;
	}
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.web.mvc.rest.v1.model.SuperclassModel#getType()
	 */
	public String getZeroLabel() {
		return zeroLabel;
	}

	public void setZeroLabel(String zeroLabel) {
		this.zeroLabel = zeroLabel;
	}

	public String getZeroColour() {
		return zeroColour;
	}

	public void setZeroColour(String zeroColour) {
		this.zeroColour = zeroColour;
	}

	public String getOneLabel() {
		return oneLabel;
	}

	public void setOneLabel(String oneLabel) {
		this.oneLabel = oneLabel;
	}

	public String getOneColour() {
		return oneColour;
	}

	public void setOneColour(String oneColour) {
		this.oneColour = oneColour;
	}

	@Override
	public String getType() {
		return BinaryTextRenderer.getDefinition().getName();
	}

}
