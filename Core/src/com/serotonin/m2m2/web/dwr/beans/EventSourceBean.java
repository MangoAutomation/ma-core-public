/*
 * Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
 * 
 * @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.dwr.beans;

import java.util.ArrayList;
import java.util.List;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.vo.event.AbstractEventHandlerVO;
import com.serotonin.m2m2.vo.event.EventTypeVO;

public class EventSourceBean {
    private int id;
    private String name;
    private final List<EventTypeVoHandlers> eventTypes = new ArrayList<EventTypeVoHandlers>();

    public List<EventTypeVoHandlers> getEventTypes() {
        return eventTypes;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static class EventTypeVoHandlers extends EventTypeVO {


        private List<AbstractEventHandlerVO<?>> handlers;

        public EventTypeVoHandlers(EventType eventType, TranslatableMessage description,
            int alarmLevel) {
            super(eventType, description, alarmLevel);
        }
        
        public List<AbstractEventHandlerVO<?>> getHandlers() {
            return handlers;
        }

        public void setHandlers(List<AbstractEventHandlerVO<?>> handlers) {
            this.handlers = handlers;
        }


    }
}
