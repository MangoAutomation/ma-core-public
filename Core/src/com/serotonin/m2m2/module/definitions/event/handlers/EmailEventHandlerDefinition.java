/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.module.definitions.event.handlers;

import com.serotonin.m2m2.module.EventHandlerDefinition;
import com.serotonin.m2m2.vo.event.EmailEventHandlerVO;

/**
 * @author Terry Packer
 *
 */
public class EmailEventHandlerDefinition extends EventHandlerDefinition<EmailEventHandlerVO>{

	public static final String TYPE_NAME = "EMAIL";
	public static final String DESC_KEY = "eventHandlers.type.email";
	public static final int EMAIL_SCRIPT_TYPE = 2;
	
	@Override
	public String getEventHandlerTypeName() {
		return TYPE_NAME;
	}

	@Override
	public String getDescriptionKey() {
		return DESC_KEY;
	}

	@Override
	protected EmailEventHandlerVO createEventHandlerVO() {
		return new EmailEventHandlerVO();
	}

}
