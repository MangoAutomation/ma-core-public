/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.events.detectors;

import com.serotonin.m2m2.vo.event.detector.NoChangeDetectorVO;

/**
 * 
 * @author Terry Packer
 */
public class NoChangeEventDetectorModel extends TimeoutDetectorModel<NoChangeDetectorVO>{

	public NoChangeEventDetectorModel(NoChangeDetectorVO data) {
		super(data);
	}

}
