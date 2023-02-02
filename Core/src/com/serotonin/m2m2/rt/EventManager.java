/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.EventManagerListenerDefinition;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.EventInstance;
import com.serotonin.m2m2.rt.event.ReturnCause;
import com.serotonin.m2m2.rt.event.UserEventListener;
import com.serotonin.m2m2.rt.event.handlers.EventHandlerInterface;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.util.ILifecycle;


/**
 *
 * @author Terry Packer
 */
public interface EventManager extends ILifecycle {

    //
    //
    // Basic event management.
    //
    /**
     * Raise Event
     * @param rtnApplicable - does this event return to normal?
     */
    void raiseEvent(EventType type, long time, boolean rtnApplicable, AlarmLevels alarmLevel,
            TranslatableMessage message, Map<String, Object> context);

    void returnToNormal(EventType type, long time);
    void returnToNormal(EventType type, long time, ReturnCause cause);

    /**
     * Acknowledges an event given an event ID.
     *
     * The returned EventInstance is a copy from the database, never the cached instance. If the returned instance
     * has a different time, userId or alternateAckSource to what was provided then the event must have been already acknowledged.
     *
     * @return the EventInstance for the ID if found, null otherwise
     */
    public EventInstance acknowledgeEventById(int eventId, long time, User user, TranslatableMessage alternateAckSource);

    /**
     * Purge All Events We have
     */
    int purgeAllEvents();

    /**
     * Purge events prior to time
     */
    int purgeEventsBefore(long time);

    /**
     * Purge Events before time with a given type
     */
    int purgeEventsBefore(long time, String typeName);

    /**
     * Purge Events before time with a given type
     */
    int purgeEventsBefore(long time, AlarmLevels alarmLevel);

    //
    //
    // Canceling events.
    //
    /**
     * Cancel active events for a Data Point
     */
    void cancelEventsForDataPoint(int dataPointId);

    /**
     * Cancel active events for these Data Points
     */
    void cancelEventsForDataPoints(Set<Integer> pointIds);

    /**
     * Cancel active events for a Data Source
     */
    void cancelEventsForDataSource(int dataSourceId);

    /**
     * Cancel all events for a publisher
     */
    void cancelEventsForPublisher(int publisherId);

    //
    //
    // Lifecycle interface
    //
    @Override
    void initialize(boolean safe);

    @Override
    void terminate();

    @Override
    void joinTermination();

    //
    //
    // Listeners
    //
    void addListener(EventManagerListenerDefinition l);

    void removeListener(EventManagerListenerDefinition l);

    void addUserEventListener(UserEventListener l);

    void removeUserEventListener(UserEventListener l);

    void addHandler(EventHandlerInterface handler);

    void removeHandler(EventHandlerInterface handler);

    /**
     * Get all active user events that a user has permission for
     */
    List<EventInstance> getAllActiveUserEvents(PermissionHolder user);

    /**
     * To access all active events quickly
     */
    List<EventInstance> getAllActive();

}
