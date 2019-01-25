/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.events.detectors;

import com.serotonin.m2m2.vo.event.detector.AnalogRangeDetectorVO;

/**
 * 
 * @author Terry Packer
 */
public class AnalogRangeEventDetectorModel extends TimeoutDetectorModel<AnalogRangeDetectorVO>{
	
	public AnalogRangeEventDetectorModel(AnalogRangeDetectorVO data) {
		super(data);
	}
	
	public double getLow() {
		return this.data.getLow();
	}

	public void setLow(double low) {
		this.data.setLow(low);
	}

	public double getHigh() {
		return this.data.getHigh();
	}

	public void setHigh(double high) {
		this.data.setHigh(high);
	}

	public boolean isWithinRange() {
		return this.data.isWithinRange();
	}

	public void setWithinRange(boolean withinRange) {
		this.data.setWithinRange(withinRange);
	}

}