/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.rt;

import java.util.List;
import java.util.Map;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.EventManagerListenerDefinition;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.EventInstance;
import com.serotonin.m2m2.rt.event.ReturnCause;
import com.serotonin.m2m2.rt.event.UserEventListener;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.util.ILifecycle;


/**
 *
 * @author Terry Packer
 */
public interface EventManager extends ILifecycle{

    /**
     * Check the state of the EventManager
     *  useful if you are a task that may run before/after the RUNNING state
     * @return
     */
    int getState();

    //
    //
    // Basic event management.
    //
    /**
     * Raise Event
     * @param type
     * @param time
     * @param rtnApplicable - does this event return to normal?
     * @param alarmLevel
     * @param message
     * @param context
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
     * @param eventId
     * @param time
     * @param user
     * @param alternateAckSource
     * @return the EventInstance for the ID if found, null otherwise
     */
    public EventInstance acknowledgeEventById(int eventId, long time, User user, TranslatableMessage alternateAckSource);

    /**
     * Get the latest alarm's timestamp
     * @return
     */
    long getLastAlarmTimestamp();

    /**
     * Purge All Events We have
     * @return
     */
    int purgeAllEvents();

    /**
     * Purge events prior to time
     * @param time
     * @return
     */
    int purgeEventsBefore(long time);

    /**
     * Purge Events before time with a given type
     * @param time
     * @param typeName
     * @return
     */
    int purgeEventsBefore(long time, String typeName);

    /**
     * Purge Events before time with a given type
     * @param time
     * @param typeName
     * @return
     */
    int purgeEventsBefore(long time, AlarmLevels alarmLevel);

    //
    //
    // Canceling events.
    //
    void cancelEventsForDataPoint(int dataPointId);

    /**
     * Cancel active events for a Data Source
     * @param dataSourceId
     */
    void cancelEventsForDataSource(int dataSourceId);

    /**
     * Cancel all events for a publisher
     * @param publisherId
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

    /**
     * Get all active user events that a user has permission for
     * @param user
     * @return
     */
    List<EventInstance> getAllActiveUserEvents(PermissionHolder user);

    /**
     * To access all active events quickly
     * @param type
     * @return
     */
    List<EventInstance> getAllActive();

}
