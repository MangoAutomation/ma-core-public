/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.module.definitions.event.handlers;

import com.serotonin.m2m2.module.EventHandlerDefinition;
import com.serotonin.m2m2.vo.event.SetPointEventHandlerVO;

/**
 * @author Terry Packer
 *
 */
public class SetPointEventHandlerDefinition extends EventHandlerDefinition<SetPointEventHandlerVO>{
	
	public static final String TYPE_NAME = "SET_POINT";
	public static final String DESC_KEY = "eventHandlers.type.setPoint";
	public static final int ACTIVE_SCRIPT_TYPE = 0;
	public static final int INACTIVE_SCRIPT_TYPE = 1;
	
	@Override
	public String getEventHandlerTypeName() {
		return TYPE_NAME;
	}

	@Override
	public String getDescriptionKey() {
		return DESC_KEY;
	}

	@Override
	protected SetPointEventHandlerVO createEventHandlerVO() {
		return new SetPointEventHandlerVO();
	}

}
