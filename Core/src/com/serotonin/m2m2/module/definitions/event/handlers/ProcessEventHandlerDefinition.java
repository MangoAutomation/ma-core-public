/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.module.definitions.event.handlers;

import com.serotonin.m2m2.module.EventHandlerDefinition;
import com.serotonin.m2m2.vo.event.ProcessEventHandlerVO;

/**
 * @author Terry Packer
 *
 */
public class ProcessEventHandlerDefinition extends EventHandlerDefinition<ProcessEventHandlerVO>{
	
	public static final String TYPE_NAME = "PROCESS";
	public static final String DESC_KEY = "eventHandlers.type.process";
	
	@Override
	public String getEventHandlerTypeName() {
		return TYPE_NAME;
	}

	@Override
	public String getDescriptionKey() {
		return DESC_KEY;
	}
	@Override
	protected ProcessEventHandlerVO createEventHandlerVO() {
		return new ProcessEventHandlerVO();
	}

}
