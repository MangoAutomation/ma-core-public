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
    /**
     * Alarm levels can be changed via system settings
     */
    private volatile int alarmLevel;

    public EventTypeVO(EventType eventType, TranslatableMessage description, int alarmLevel) {
        this.eventType = eventType;
        this.description = description;
        this.alarmLevel = alarmLevel;
    }

    public EventType getEventType() {
        return eventType;
    }

    /**
     * Use getEventType().get... instead
     * @return
     */
    @Deprecated
    public String getType() {
        return eventType.getEventType();
    }

    /**
     * Use getEventType().get... instead
     * @return
     */
    @Deprecated
    public String getSubtype() {
        return eventType.getEventSubtype();
    }

    /**
     * Use getEventType().get... instead
     * @return
     */
    @Deprecated
    public int getTypeRef1() {
        return eventType.getReferenceId1();
    }

    /**
     * Use getEventType().get... instead
     * @return
     */
    @Deprecated
    public int getTypeRef2() {
        return eventType.getReferenceId2();
    }

    public TranslatableMessage getDescription() {
        return description;
    }

    public int getAlarmLevel() {
        return alarmLevel;
    }

    /**
     * Alarm levels can be changed via system settings
     *
     * @param alarmLevel
     */
    public void setAlarmLevel(int alarmLevel) {
        this.alarmLevel = alarmLevel;
    }
}
