/*
 * Copyright (C) 2019 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.rt.event.handlers;

import java.util.Collections;
import java.util.Map;

import javax.script.ScriptEngine;

import com.infiniteautomation.mango.spring.service.ScriptExecutor;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.rt.event.EventInstance;
import com.serotonin.m2m2.vo.event.ScriptEventHandlerVO;

/**
 * @author Jared Wiltshire
 */
public class ScriptEventHandlerRT extends EventHandlerRT<ScriptEventHandlerVO> {

    final EventHandlerInterface scriptHandlerDelegate;

    public ScriptEventHandlerRT(ScriptEventHandlerVO vo) {
        super(vo);

        ScriptExecutor scriptExecutor = Common.getBean(ScriptExecutor.class);

        Map<String, Object> bindings = Collections.singletonMap("eventHandler", vo);

        ScriptEngine engine = scriptExecutor.executeScript(vo.getName(), vo.getEngineName(), vo.getScript(), bindings);
        this.scriptHandlerDelegate = scriptExecutor.getInterface(engine, EventHandlerInterface.class);
    }

    @Override
    public void eventRaised(EventInstance evt) {
        this.scriptHandlerDelegate.eventRaised(evt);
    }

    @Override
    public void eventInactive(EventInstance evt) {
        this.scriptHandlerDelegate.eventInactive(evt);
    }

}
