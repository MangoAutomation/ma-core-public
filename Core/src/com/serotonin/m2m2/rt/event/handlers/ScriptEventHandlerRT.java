/*
 * Copyright (C) 2019 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.rt.event.handlers;

import java.util.Collections;

import com.infiniteautomation.mango.spring.script.ScriptService;
import com.infiniteautomation.mango.spring.script.StringMangoScript;
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

        ScriptService scriptService = Common.getBean(ScriptService.class);
        StringMangoScript script = new StringMangoScript(vo.getEngineName(), vo.getName(), vo.getScript());
        script.setBindings(Collections.singletonMap("eventHandler", vo));
        this.scriptHandlerDelegate = scriptService.getInterface(script, EventHandlerInterface.class);
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
