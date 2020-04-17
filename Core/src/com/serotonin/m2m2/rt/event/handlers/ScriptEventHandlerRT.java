/*
 * Copyright (C) 2019 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.rt.event.handlers;

import java.util.Collections;

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

    public ScriptEventHandlerRT(ScriptEventHandlerVO vo) {
        super(vo);

        PermissionService permissionService = Common.getBean(PermissionService.class);
        this.scriptHandlerDelegate = permissionService.runAsSystemAdmin(() -> {
            ScriptService scriptService = Common.getBean(ScriptService.class);
            return scriptService.getInterface(vo.toMangoScript(), EventHandlerInterface.class, Collections.singletonMap(EVENT_HANDLER_KEY, vo));
        });
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
