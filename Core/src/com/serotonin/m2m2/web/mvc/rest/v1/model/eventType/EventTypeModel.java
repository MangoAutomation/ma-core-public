/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.eventType;

import com.serotonin.m2m2.rt.event.type.EventType;

/**
 *
 * @author Terry Packer
 */
public abstract class EventTypeModel{

    /**
     * Type of Event Model
     * @return
     */
    abstract public String getTypeName();

    abstract public boolean isRateLimited();

    abstract public String getDuplicateHandling();

    /**
     * Converts the model to an event type
     * @return
     */
    abstract public EventType toEventType();

}
