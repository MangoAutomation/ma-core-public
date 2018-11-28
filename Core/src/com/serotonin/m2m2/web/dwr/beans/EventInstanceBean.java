/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.web.dwr.beans;

import com.serotonin.m2m2.rt.event.AlarmLevels;

/**
 * @author Matthew Lohbihler
 */
public class EventInstanceBean {
    private final boolean active;
    private final AlarmLevels alarmLevel;
    private final String prettyActiveTimestamp;
    private final String message;

    public EventInstanceBean(boolean active, AlarmLevels alarmLevel, String prettyActiveTimestamp, String message) {
        this.active = active;
        this.alarmLevel = alarmLevel;
        this.prettyActiveTimestamp = prettyActiveTimestamp;
        this.message = message;
    }

    public boolean isActive() {
        return active;
    }

    public AlarmLevels getAlarmLevel() {
        return alarmLevel;
    }

    public String getPrettyActiveTimestamp() {
        return prettyActiveTimestamp;
    }

    public String getMessage() {
        return message;
    }
}
