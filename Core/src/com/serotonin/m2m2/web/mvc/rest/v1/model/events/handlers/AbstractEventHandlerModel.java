/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.events.handlers;

import com.serotonin.m2m2.vo.event.AbstractEventHandlerVO;
import com.serotonin.m2m2.web.mvc.rest.v1.model.AbstractVoModel;

/**
 * TODO Implement fully
 * @author Terry Packer
 *
 */
public abstract class AbstractEventHandlerModel <T extends AbstractEventHandlerVO<?>> extends AbstractVoModel<AbstractEventHandlerVO<?>>{

	/**
	 * @param data
	 */
	public AbstractEventHandlerModel(AbstractEventHandlerVO<?> data) {
		super(data);
	}
	

}
