/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.events.detectors;

import com.serotonin.m2m2.vo.event.detector.BinaryStateDetectorVO;

/**
 * 
 * @author Terry Packer
 */
public class BinaryStateEventDetectorModel extends TimeoutDetectorModel<BinaryStateDetectorVO>{

	public BinaryStateEventDetectorModel() {
		super(new BinaryStateDetectorVO());
	}
	
	public BinaryStateEventDetectorModel(BinaryStateDetectorVO data) {
		super(data);
	}

	public boolean isState(){
		return this.data.isState();
	}
	
	public void setState(boolean state){
		this.data.setState(state);
	}
	
}
