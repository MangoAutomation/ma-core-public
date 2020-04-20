/*
 * Copyright (C) 2019 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.rt.event.handlers;

import java.util.Collections;

import com.infiniteautomation.mango.spring.script.MangoScript;
import com.infiniteautomation.mango.spring.script.ScriptService;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.rt.event.EventInstance;
import com.serotonin.m2m2.vo.event.ScriptEventHandlerVO;

/**
 * @author Jared Wiltshire
 */
public class ScriptEventHandlerRT extends EventHandlerRT<ScriptEventHandlerVO> {

    public static final String EVENT_HANDLER_KEY = "eventHandler";

    final EventHandlerInterface scriptHandlerDelegate;
    final PermissionService permissionService;
    final MangoScript script;

    public ScriptEventHandlerRT(ScriptEventHandlerVO vo) {
        super(vo);

        ScriptService scriptService = Common.getBean(ScriptService.class);
        this.permissionService = Common.getBean(PermissionService.class);
        this.script = vo.toMangoScript();

        this.scriptHandlerDelegate = permissionService.runAsSystemAdmin(() -> {
            return scriptService.getInterface(script, EventHandlerInterface.class, Collections.singletonMap(EVENT_HANDLER_KEY, vo));
        });
    }

    @Override
    public void eventRaised(EventInstance evt) {
        permissionService.runAs(script, () -> {
            this.scriptHandlerDelegate.eventRaised(evt);
        });
    }

    @Override
    public void eventInactive(EventInstance evt) {
        permissionService.runAs(script, () -> {
            this.scriptHandlerDelegate.eventInactive(evt);
        });
    }

}
