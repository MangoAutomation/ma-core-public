/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.events.detectors;

import com.serotonin.m2m2.vo.event.detector.AnalogLowLimitDetectorVO;

/**
 * 
 * @author Terry Packer
 */
public class AnalogLowLimitEventDetectorModel extends TimeoutDetectorModel<AnalogLowLimitDetectorVO>{

	public AnalogLowLimitEventDetectorModel() {
		super(new AnalogLowLimitDetectorVO());
	}
	
	public AnalogLowLimitEventDetectorModel(AnalogLowLimitDetectorVO data) {
		super(data);
	}

	public double getLimit() {
		return this.data.getLimit();
	}

	public void setLimit(double limit) {
		this.data.setLimit(limit);
	}

	public double getResetLimit() {
		return this.data.getResetLimit();
	}

	public void setResetLimit(double resetLimit) {
		this.data.setResetLimit(resetLimit);
	}
	
	public boolean isUseResetLimit() {
		return this.data.isUseResetLimit();
	}

	public void setUseResetLimit(boolean useResetLimit) {
		this.data.setUseResetLimit(useResetLimit);
	}
	
	public boolean isNotLower() {
		return this.data.isNotLower();
	}

	public void setNotLower(boolean notLower) {
		this.data.setNotLower(notLower);
	}
}