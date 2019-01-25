/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.events.detectors;

import com.serotonin.m2m2.vo.event.detector.SmoothnessDetectorVO;

/**
 * 
 * @author Terry Packer
 */
public class SmoothnessDetectorEventDetectorModel extends TimeoutDetectorModel<SmoothnessDetectorVO>{

	public SmoothnessDetectorEventDetectorModel(SmoothnessDetectorVO data) {
		super(data);
	}

	public double getLimit() {
		return this.data.getLimit();
	}

	public void setLimit(double limit) {
		this.data.setLimit(limit);
	}

	public double getBoxcar() {
		return this.data.getBoxcar();
	}

	public void setBoxcar(double boxcar) {
		this.data.setBoxcar(boxcar);
	}
}
