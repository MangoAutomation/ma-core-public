/*
 * Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
 *
 * @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.dwr.beans;

import java.util.ArrayList;
import java.util.List;

/**
 * Used on legacy pages via the EventHandlersDwr
 */
public class EventSourceBean {
    private int id;
    private String name;
    private final List<EventTypeVOHandlers> eventTypes = new ArrayList<EventTypeVOHandlers>();

    public List<EventTypeVOHandlers> getEventTypes() {
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
}
