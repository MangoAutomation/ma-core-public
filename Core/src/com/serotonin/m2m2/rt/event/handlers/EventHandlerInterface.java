/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.event.handlers;

import java.util.Map;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.EventManagerImpl;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.EventInstance;
import com.serotonin.m2m2.rt.event.type.EventType;

/**
 * @author Jared Wiltshire
 */
public interface EventHandlerInterface {

    /**
     * Called when an event is raised. This method will not be called for duplicate events, events with IGNORE level,
     * or events which are auto-acknowledged. Note that not all events that are raised are considered active,
     * some events are instantaneous and do not return to normal.
     *
     * See {@link EventManagerImpl#raiseEvent(EventType, long, boolean, AlarmLevels, TranslatableMessage, Map) raiseEvent(...)}
     *
     * @param evt event instance
     */
    void eventRaised(EventInstance evt);

    /**
     * Called when the event is acknowledged. All events can be acknowledged, including instantaneous events.
     * Events which return to normal may be active or inactive when they are acknowledged.
     *
     * @param evt event instance
     */
    default void eventAcknowledged(EventInstance evt) {}

    /**
     * Called when the event becomes inactive (returns to normal, or is cancelled on termination of a data source).
     * Instantaneous events which do not return to normal never become inactive.
     *
     * See {@link EventManagerImpl#raiseEvent(EventType, long, boolean, AlarmLevels, TranslatableMessage, Map) raiseEvent(...)}
     *
     * @param evt event instance
     */
    void eventInactive(EventInstance evt);
}
