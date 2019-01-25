/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.events.detectors;

import com.serotonin.m2m2.vo.event.detector.StateChangeCountDetectorVO;

/**
 * 
 * @author Terry Packer
 */
public class StateChangeCountEventDetectorModel extends TimeoutDetectorModel<StateChangeCountDetectorVO>{
	
	public StateChangeCountEventDetectorModel(StateChangeCountDetectorVO data) {
		super(data);
	}

	public int getChangeCount() {
		return this.data.getChangeCount();
	}

	public void setChangeCount(int changeCount) {
		this.data.setChangeCount(changeCount);
	}
}
