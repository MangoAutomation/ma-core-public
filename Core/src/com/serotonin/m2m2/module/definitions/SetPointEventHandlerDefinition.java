/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.module.definitions;

import com.serotonin.m2m2.module.EventHandlerDefinition;
import com.serotonin.m2m2.vo.event.AbstractEventHandlerVO;
import com.serotonin.m2m2.vo.event.SetPointEventHandlerVO;

/**
 * @author Terry Packer
 *
 */
public class SetPointEventHandlerDefinition extends EventHandlerDefinition{
	
	public static final String TYPE_NAME = "SET_POINT";
	public static final String DESC_KEY = "eventHandlers.type.setPoint";
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.EventHandlerDefinition#getEventHandlerTypeName()
	 */
	@Override
	public String getEventHandlerTypeName() {
		return TYPE_NAME;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.EventHandlerDefinition#getDescriptionKey()
	 */
	@Override
	public String getDescriptionKey() {
		return DESC_KEY;
	}
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.EventHandlerDefinition#createEventHandlerVO()
	 */
	@Override
	protected AbstractEventHandlerVO createEventHandlerVO() {
		return new SetPointEventHandlerVO();
	}
}
