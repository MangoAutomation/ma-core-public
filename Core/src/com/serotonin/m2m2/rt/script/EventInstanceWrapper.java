package com.serotonin.m2m2.rt.script;

import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.EventInstance;
import com.serotonin.m2m2.rt.event.type.EventType;

public class EventInstanceWrapper {
    private final EventInstance event;

    public EventInstanceWrapper(EventInstance event) {
        this.event = event;
    }

    public int getEventId() {
        return event.getId();
    }
    public EventType getEventTypeObject() {
        return event.getEventType();
    }
    public String getType() {
        return event.getEventType().getEventType();
    }
    public String getSubtype() {
        return event.getEventType().getEventSubtype();
    }
    public int getReferenceId1() {
        return event.getEventType().getReferenceId1();
    }
    public int getReferenceId2() {
        return event.getEventType().getReferenceId2();
    }
    public long getActiveTimestamp() {
        return event.getActiveTimestamp();
    }
    public long getReturnTimestamp() {
        return event.getRtnTimestamp();
    }
    public boolean isReturnApplicable() {
        return event.isRtnApplicable();
    }
    public AlarmLevels getAlarmLevel() {
        return event.getAlarmLevel();
    }
    public String getMessage() {
        return event.getMessageString();
    }
    public String getMessageKey() {
        return event.getMessage().getKey();
    }
    public boolean isActive() {
        return event.isActive();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");

        builder.append("getEventId(): int,\n");
        builder.append("getEventTypeObject(): EventType,\n");
        builder.append("getType(): String,\n");
        builder.append("getSubtype(): String,\n");
        builder.append("getReferenceId1(): int,\n");
        builder.append("getReferenceId2(): int,\n");
        builder.append("getActiveTimestamp(): long,\n");
        builder.append("getReturnTimestamp(): long,\n");
        builder.append("isReturnApplicable(): boolean,\n");
        builder.append("getAlarmLevel(): int,\n");
        builder.append("getMessage(): String,\n");
        builder.append("getMessageKey(): String,\n");
        builder.append("isActive(): boolean,\n");

        builder.append(" }\n");
        return builder.toString();
    }
}