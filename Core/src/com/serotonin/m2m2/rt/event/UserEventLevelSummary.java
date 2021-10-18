/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.event;

import com.serotonin.m2m2.vo.event.EventInstanceI;

/**
 * Provides a summary for an event level of all
 *  unsilenced events for a user.
 * @author Terry Packer
 */
public class UserEventLevelSummary {

    private final AlarmLevels alarmLevel;
    private int count;
    private EventInstanceI latest;

    public UserEventLevelSummary(AlarmLevels alarmLevel) {
        this.alarmLevel = alarmLevel;
    }

    public UserEventLevelSummary(AlarmLevels alarmLevel, int count) {
        this.alarmLevel = alarmLevel;
        this.count = count;
    }

    public UserEventLevelSummary(AlarmLevels alarmLevel, int count, EventInstanceI latest) {
        this.alarmLevel = alarmLevel;
        this.count = count;
        this.latest = latest;
    }

    /**
     * @return the alarmLevel
     */
    public AlarmLevels getAlarmLevel() {
        return alarmLevel;
    }

    /**
     * @return the count
     */
    public int getCount() {
        return count;
    }

    /**
     * @return the latest
     */
    public EventInstanceI getLatest() {
        return latest;
    }

    /**
     * Increment the count and set the latest event instance
     * @param latest
     */
    public void increment(EventInstanceI latest) {
        this.count++;

        if (this.latest == null || this.latest.getActiveTimestamp() <= latest.getActiveTimestamp()) {
            this.latest = latest;
        }
    }

}
