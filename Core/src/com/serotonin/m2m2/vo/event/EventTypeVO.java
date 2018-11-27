/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.vo.event;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.type.EventType;

public class EventTypeVO {
    private final EventType eventType;
    private final TranslatableMessage description;
    private final int alarmLevel;

    public EventTypeVO(EventType eventType, TranslatableMessage description, int alarmLevel) {
        this.eventType = eventType;
        this.description = description;
        this.alarmLevel = alarmLevel;
    }

    public EventType getEventType() {
        return eventType;
    }

    public TranslatableMessage getDescription() {
        return description;
    }

    public int getAlarmLevel() {
        return alarmLevel;
    }
}
