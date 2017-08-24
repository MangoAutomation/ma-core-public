/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.rt;

import java.util.List;
import java.util.Map;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.EventManagerListenerDefinition;
import com.serotonin.m2m2.rt.event.EventInstance;
import com.serotonin.m2m2.rt.event.UserEventListener;
import com.serotonin.m2m2.rt.event.type.EventType;


/**
 *
 * @author Terry Packer
 */
public interface EventManager {

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
    void raiseEvent(EventType type, long time, boolean rtnApplicable, int alarmLevel,
            TranslatableMessage message, Map<String, Object> context);

    void returnToNormal(EventType type, long time);

    void returnToNormal(EventType type, long time, int alarmLevel);

    void returnToNormal(EventType type, long time, int alarmLevel, int cause);

    /**
     * Added to allow Acknowledge Events to be fired
     * @param evt
     * @param time
     * @param userId
     * @param alternateAckSource
     */
    void acknowledgeEvent(EventInstance evt, long time, int userId,
            TranslatableMessage alternateAckSource);

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
    int purgeEventsBefore(long time, int alarmLevel);

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
    void initialize(boolean safe);

    void terminate();

    void joinTermination();

    //
    //
    // Listeners
    //
    void addListener(EventManagerListenerDefinition l);

    void removeListener(EventManagerListenerDefinition l);

    void addUserEventListener(UserEventListener l);

    void removeUserEventListener(UserEventListener l);

    //
    // User Event Cache Access
    //
    List<EventInstance> getAllActiveUserEvents(int userId);

    /**
     * To access all active events quickly
     * @param type
     * @return
     */
    List<EventInstance> getAllActive();

}
