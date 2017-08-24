/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.events.detectors;

import com.serotonin.m2m2.vo.event.detector.AnalogChangeDetectorVO;

/**
 * 
 * @author Terry Packer
 */
public class AnalogChangeEventDetectorModel extends TimeoutDetectorModel<AnalogChangeDetectorVO>{

	public AnalogChangeEventDetectorModel(AnalogChangeDetectorVO data) {
		super(data);
	}

	public AnalogChangeEventDetectorModel() {
		super(new AnalogChangeDetectorVO());
	}

	public double getLimit() {
		return this.data.getLimit();
	}

	public void setLimit(double limit) {
		this.data.setLimit(limit);
	}
}
