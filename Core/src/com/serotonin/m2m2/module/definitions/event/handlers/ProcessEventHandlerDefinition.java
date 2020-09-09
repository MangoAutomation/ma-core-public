/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.module.definitions.event.handlers;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.module.EventHandlerDefinition;
import com.serotonin.m2m2.rt.event.handlers.EventHandlerRT;
import com.serotonin.m2m2.rt.event.handlers.ProcessHandlerRT;
import com.serotonin.m2m2.vo.event.ProcessEventHandlerVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

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

    @Override
    public void validate(ProcessResult result, ProcessEventHandlerVO vo, PermissionHolder savingUser) {
        if (StringUtils.isBlank(vo.getActiveProcessCommand()) && StringUtils.isBlank(vo.getInactiveProcessCommand()))
            result.addGenericMessage("eventHandlers.invalidCommands");

        if (!StringUtils.isBlank(vo.getActiveProcessCommand()) && vo.getActiveProcessTimeout() <= 0)
            result.addGenericMessage("validate.greaterThanZero");

        if (!StringUtils.isBlank(vo.getInactiveProcessCommand()) && vo.getInactiveProcessTimeout() <= 0)
            result.addGenericMessage("validate.greaterThanZero");
    }

	@Override
	public EventHandlerRT<ProcessEventHandlerVO> createRuntime(ProcessEventHandlerVO vo){
		return new ProcessHandlerRT(vo);
	}
}
