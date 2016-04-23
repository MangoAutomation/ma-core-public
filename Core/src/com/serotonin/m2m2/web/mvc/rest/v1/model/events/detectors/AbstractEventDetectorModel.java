/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.events.detectors;

import com.serotonin.m2m2.vo.event.detector.AbstractEventDetectorVO;
import com.serotonin.m2m2.web.mvc.rest.v1.model.AbstractVoModel;

/**
 * TODO Implement fully
 * @author Terry Packer
 *
 */
public abstract class AbstractEventDetectorModel<T extends AbstractEventDetectorVO<?>> extends AbstractVoModel<AbstractEventDetectorVO<?>>{

	/**
	 * @param data
	 */
	public AbstractEventDetectorModel(AbstractEventDetectorVO<?> data) {
		super(data);
	}
	
	

}
