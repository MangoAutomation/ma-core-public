/*
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.web.dwr.beans;

import java.util.List;

import com.serotonin.m2m2.vo.event.AbstractEventHandlerVO;
import com.serotonin.m2m2.vo.event.EventTypeVO;

/**
 * EventTypeVO with getters and setters added for the event type, coupled with event handlers for this event type
 */
public class EventTypeVOHandlers extends EventTypeVO {

    private List<AbstractEventHandlerVO<?>> handlers;

    public EventTypeVOHandlers(EventTypeVO delegate) {
        super(delegate.getEventType(), delegate.getDescription(), delegate.getAlarmLevel());
    }

    public EventTypeVOHandlers(EventTypeVO delegate, List<AbstractEventHandlerVO<?>> handlers) {
        super(delegate.getEventType(), delegate.getDescription(), delegate.getAlarmLevel());
        this.handlers = handlers;
    }

    public List<AbstractEventHandlerVO<?>> getHandlers() {
        return handlers;
    }

    public void setHandlers(List<AbstractEventHandlerVO<?>> handlers) {
        this.handlers = handlers;
    }
}