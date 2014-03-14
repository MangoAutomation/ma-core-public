/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.event.handlers;

import com.serotonin.m2m2.rt.EventManager;
import com.serotonin.m2m2.rt.event.EventInstance;
import com.serotonin.m2m2.vo.event.EventHandlerVO;

abstract public class EventHandlerRT {
    protected EventHandlerVO vo;

    /**
     * Not all events that are raised are made active. It depends on the event's alarm level and duplicate handling.
     * 
     * @see EventManager.raiseEvent for details.
     * @param evt
     */
    abstract public void eventRaised(EventInstance evt);

    /**
     * Called when the event is considered inactive.
     * 
     * @see EventManager.raiseEvent for details.
     * @param evt
     */
    abstract public void eventInactive(EventInstance evt);
}
