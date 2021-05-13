/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.rt.event.type;

import java.util.Objects;

import org.checkerframework.checker.nullness.qual.NonNull;

public class EventTypeMatcher {

    private String eventType;
    private String eventSubtype;
    private int referenceId1;
    private int referenceId2;

    public EventTypeMatcher() {
    }

    public EventTypeMatcher(@NonNull EventType other) {
        this(other.getEventType(), other.getEventSubtype() == null ? "" : other.getEventSubtype(), other.getReferenceId1(), other.getReferenceId2());
    }

    /**
     * @param eventType may be empty to match any event type, must be non null
     * @param eventSubtype may be empty to match any event subtype, must be non null
     * @param referenceId1 may be 0 to match any referenceId1
     * @param referenceId2 may be 0 to match any referenceId2
     */
    public EventTypeMatcher(@NonNull String eventType, @NonNull String eventSubtype, int referenceId1, int referenceId2) {
        this.eventType = eventType;
        this.eventSubtype = eventSubtype;
        this.referenceId1 = referenceId1;
        this.referenceId2 = referenceId2;
    }

    /**
     * @param type event type to match
     * @return true if event type matches
     */
    public boolean matches(EventType type) {
        return (eventType.isEmpty() || eventType.equals(type.getEventType())) &&
                (eventSubtype.isEmpty() || eventSubtype.equals(type.getEventSubtype())) &&
                (referenceId1 == 0 || referenceId1 == type.getReferenceId1()) &&
                (referenceId2 == 0 || referenceId2 == type.getReferenceId2());
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEventSubtype() {
        return eventSubtype;
    }

    public void setEventSubtype(String eventSubtype) {
        this.eventSubtype = eventSubtype;
    }

    public int getReferenceId1() {
        return referenceId1;
    }

    public void setReferenceId1(int referenceId1) {
        this.referenceId1 = referenceId1;
    }

    public int getReferenceId2() {
        return referenceId2;
    }

    public void setReferenceId2(int referenceId2) {
        this.referenceId2 = referenceId2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventTypeMatcher that = (EventTypeMatcher) o;
        return referenceId1 == that.referenceId1 && referenceId2 == that.referenceId2 && Objects.equals(eventType, that.eventType) && Objects.equals(eventSubtype, that.eventSubtype);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventType, eventSubtype, referenceId1, referenceId2);
    }
}
