/*
 * Copyright (C) 2019 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.module.definitions.event.handlers;

import javax.script.ScriptException;

import com.infiniteautomation.mango.spring.service.ScriptExecutor.EngineNotFoundException;
import com.infiniteautomation.mango.spring.service.ScriptExecutor.ScriptEvalException;
import com.infiniteautomation.mango.spring.service.ScriptExecutor.ScriptInterfaceException;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.module.EventHandlerDefinition;
import com.serotonin.m2m2.vo.event.ScriptEventHandlerVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * @author Jared Wiltshire
 */
public class ScriptEventHandlerDefinition extends EventHandlerDefinition<ScriptEventHandlerVO> {

    public static final String TYPE_NAME = "SCRIPT";
    public static final String DESC_KEY = "eventHandlers.type.script";

    @Override
    public String getEventHandlerTypeName() {
        return TYPE_NAME;
    }

    @Override
    public String getDescriptionKey() {
        return DESC_KEY;
    }

    @Override
    protected ScriptEventHandlerVO createEventHandlerVO() {
        return new ScriptEventHandlerVO();
    }

    @Override
    public void validate(ProcessResult response, ScriptEventHandlerVO handler, PermissionHolder user) {
        String script = handler.getScript();
        String engineName = handler.getEngineName();

        if (script == null || script.isEmpty()) {
            response.addContextualMessage("script", "validate.required");
        }
        if (engineName == null || engineName.isEmpty()) {
            response.addContextualMessage("engineName", "validate.required");
        }

        try {
            handler.createRuntime();
        } catch (EngineNotFoundException e) {
            response.addContextualMessage("engineName", "validate.invalidValueWithAcceptable", e.getAvailableEngines());
        } catch (ScriptEvalException e) {
            ScriptException cause = e.getCause();

            if (cause.getLineNumber() < 0) {
                response.addContextualMessage("script", "script.scriptException", cause.getMessage());
            } else if (cause.getColumnNumber() < 0) {
                response.addContextualMessage("script", "script.scriptExceptionLine", cause.getMessage(), cause.getLineNumber());
            } else {
                response.addContextualMessage("script", "script.scriptExceptionLineColumn", cause.getMessage(), cause.getLineNumber(), cause.getColumnNumber());
            }
        } catch (ScriptInterfaceException e) {
            response.addContextualMessage("script", "script.cantGetInterface", e.getInterfaceClass().getName());
        }
    }

}
