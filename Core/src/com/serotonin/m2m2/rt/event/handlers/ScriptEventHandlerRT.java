/*
 * Copyright (C) 2019 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.rt.event.handlers;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.Map;

import com.infiniteautomation.mango.spring.script.EvalContext;
import com.infiniteautomation.mango.spring.script.ScriptService;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.rt.event.EventInstance;
import com.serotonin.m2m2.vo.event.ScriptEventHandlerVO;

/**
 * @author Jared Wiltshire
 */
public class ScriptEventHandlerRT extends EventHandlerRT<ScriptEventHandlerVO> {

    public static final String EVENT_HANDLER_KEY = "eventHandler";

    final EventHandlerInterface scriptHandlerDelegate;

    public ScriptEventHandlerRT(ScriptEventHandlerVO vo) {
        super(vo);

        ScriptService scriptService = Common.getBean(ScriptService.class);
        Map<String, Object> bindings = Collections.singletonMap(EVENT_HANDLER_KEY, vo);

        EvalContext context = new EvalContext(bindings);
        context.setWriter(new PrintWriter(System.out));
        context.setErrorWriter(new PrintWriter(System.err));

        this.scriptHandlerDelegate = scriptService.getInterface(
                vo.toMangoScript(),
                EventHandlerInterface.class,
                context);
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
