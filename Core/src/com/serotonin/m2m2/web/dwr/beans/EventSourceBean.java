/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.dwr.beans;

import java.util.ArrayList;
import java.util.List;

import com.serotonin.m2m2.vo.event.EventTypeVO;

public class EventSourceBean {
    private int id;
    private String name;
    private final List<EventTypeVO> eventTypes = new ArrayList<EventTypeVO>();

    public List<EventTypeVO> getEventTypes() {
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
