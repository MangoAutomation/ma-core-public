/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.event.handlers;

import com.serotonin.m2m2.rt.EventManagerImpl;
import com.serotonin.m2m2.rt.event.EventInstance;
import com.serotonin.m2m2.vo.event.AbstractEventHandlerVO;

abstract public class EventHandlerRT<T extends AbstractEventHandlerVO> {

    protected T vo;

    public EventHandlerRT(T vo){
        this.vo = vo;
    }

    /**
     * Not all events that are raised are made active. It depends on the event's alarm level and duplicate handling.
     *
     * @see EventManagerImpl.raiseEvent for details.
     * @param evt
     */
    abstract public void eventRaised(EventInstance evt);

    /**
     * Called when the event is considered inactive.
     *
     * @see EventManagerImpl.raiseEvent for details.
     * @param evt
     */
    abstract public void eventInactive(EventInstance evt);
}
