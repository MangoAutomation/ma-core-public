/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.events.detectors;

import com.serotonin.m2m2.vo.event.detector.AlphanumericStateDetectorVO;

/**
 * 
 * @author Terry Packer
 */
public class AlphanumericStateEventDetectorModel extends TimeoutDetectorModel<AlphanumericStateDetectorVO>{

	public AlphanumericStateEventDetectorModel(AlphanumericStateDetectorVO data) {
		super(data);
	}

	public String getState() {
		return this.data.getState();
	}

	public void setState(String state) {
		this.data.setState(state);
	}
	
}
