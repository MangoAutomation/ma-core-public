/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.events.detectors;

import com.serotonin.m2m2.vo.event.detector.MultistateStateDetectorVO;

/**
 * 
 * @author Terry Packer
 */
public class MultistateStateEventDetectorModel extends TimeoutDetectorModel<MultistateStateDetectorVO>{

	public MultistateStateEventDetectorModel() {
		super(new MultistateStateDetectorVO());
	}
	
	public MultistateStateEventDetectorModel(MultistateStateDetectorVO data) {
		super(data);
	}

	public int isState(){
		return this.data.getState();
	}
	
	public void setState(int state){
		this.data.setState(state);
	}
	
}
