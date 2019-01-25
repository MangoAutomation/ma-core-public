/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.events.detectors;

import com.serotonin.m2m2.vo.event.detector.NegativeCusumDetectorVO;

/**
 * 
 * @author Terry Packer
 */
public class NegativeCusumEventDetectorModel extends TimeoutDetectorModel<NegativeCusumDetectorVO>{
	
	public NegativeCusumEventDetectorModel(NegativeCusumDetectorVO data) {
		super(data);
	}

	public double getLimit() {
		return this.data.getLimit();
	}

	public void setLimit(double limit) {
		this.data.setLimit(limit);
	}

	public double getWeight() {
		return this.data.getWeight();
	}

	public void setWeight(double weight) {
		this.data.setWeight(weight);
	}
	
}
