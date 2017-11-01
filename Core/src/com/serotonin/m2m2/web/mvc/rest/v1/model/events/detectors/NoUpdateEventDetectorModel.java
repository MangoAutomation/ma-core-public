/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.events.detectors;

import com.serotonin.m2m2.vo.event.detector.NoUpdateDetectorVO;

/**
 * 
 * @author Terry Packer
 */
public class NoUpdateEventDetectorModel extends TimeoutDetectorModel<NoUpdateDetectorVO>{

	public NoUpdateEventDetectorModel(NoUpdateDetectorVO data) {
		super(data);
	}
	
	public NoUpdateEventDetectorModel() {
		super(new NoUpdateDetectorVO());
	}
	
}
