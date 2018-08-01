/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2;

import java.util.List;
import java.util.Map;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.EventManagerListenerDefinition;
import com.serotonin.m2m2.rt.EventManager;
import com.serotonin.m2m2.rt.event.EventInstance;
import com.serotonin.m2m2.rt.event.UserEventListener;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.vo.User;

/**
 *
 * @author Terry Packer
 */
public class MockEventManager implements EventManager{

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.EventManager#getState()
     */
    @Override
    public int getState() {

        return 0;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.EventManager#raiseEvent(com.serotonin.m2m2.rt.event.type.EventType, long, boolean, int, com.serotonin.m2m2.i18n.TranslatableMessage, java.util.Map)
     */
    @Override
    public void raiseEvent(EventType type, long time, boolean rtnApplicable, int alarmLevel,
            TranslatableMessage message, Map<String, Object> context) {

        
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.EventManager#returnToNormal(com.serotonin.m2m2.rt.event.type.EventType, long)
     */
    @Override
    public void returnToNormal(EventType type, long time) {

        
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.EventManager#returnToNormal(com.serotonin.m2m2.rt.event.type.EventType, long, int)
     */
    @Override
    public void returnToNormal(EventType type, long time, int alarmLevel) {

        
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.EventManager#returnToNormal(com.serotonin.m2m2.rt.event.type.EventType, long, int, int)
     */
    @Override
    public void returnToNormal(EventType type, long time, int alarmLevel, int cause) {

        
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.EventManager#acknowledgeEventById(int, long, int, com.serotonin.m2m2.i18n.TranslatableMessage)
     */
    @Override
    public EventInstance acknowledgeEventById(int eventId, long time, User user,
            TranslatableMessage alternateAckSource) {

        return null;
    }

    /*
     * (non-Javadoc)
     * @see com.serotonin.m2m2.rt.EventManager#toggleSilence(int, int)
     */
    @Override
    public boolean toggleSilence(int eventId, int userId) {
        return false;
    }
    
    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.EventManager#getLastAlarmTimestamp()
     */
    @Override
    public long getLastAlarmTimestamp() {

        return 0;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.EventManager#purgeAllEvents()
     */
    @Override
    public int purgeAllEvents() {

        return 0;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.EventManager#purgeEventsBefore(long)
     */
    @Override
    public int purgeEventsBefore(long time) {

        return 0;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.EventManager#purgeEventsBefore(long, java.lang.String)
     */
    @Override
    public int purgeEventsBefore(long time, String typeName) {

        return 0;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.EventManager#purgeEventsBefore(long, int)
     */
    @Override
    public int purgeEventsBefore(long time, int alarmLevel) {

        return 0;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.EventManager#cancelEventsForDataPoint(int)
     */
    @Override
    public void cancelEventsForDataPoint(int dataPointId) {

        
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.EventManager#cancelEventsForDataSource(int)
     */
    @Override
    public void cancelEventsForDataSource(int dataSourceId) {

        
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.EventManager#cancelEventsForPublisher(int)
     */
    @Override
    public void cancelEventsForPublisher(int publisherId) {

        
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.EventManager#initialize(boolean)
     */
    @Override
    public void initialize(boolean safe) {

        
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.EventManager#terminate()
     */
    @Override
    public void terminate() {

        
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.EventManager#joinTermination()
     */
    @Override
    public void joinTermination() {

        
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.EventManager#addListener(com.serotonin.m2m2.module.EventManagerListenerDefinition)
     */
    @Override
    public void addListener(EventManagerListenerDefinition l) {

        
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.EventManager#removeListener(com.serotonin.m2m2.module.EventManagerListenerDefinition)
     */
    @Override
    public void removeListener(EventManagerListenerDefinition l) {

        
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.EventManager#addUserEventListener(com.serotonin.m2m2.rt.event.UserEventListener)
     */
    @Override
    public void addUserEventListener(UserEventListener l) {

        
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.EventManager#removeUserEventListener(com.serotonin.m2m2.rt.event.UserEventListener)
     */
    @Override
    public void removeUserEventListener(UserEventListener l) {

        
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.EventManager#getAllActiveUserEvents(int)
     */
    @Override
    public List<EventInstance> getAllActiveUserEvents(int userId) {

        return null;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.EventManager#getAllActive()
     */
    @Override
    public List<EventInstance> getAllActive() {

        return null;
    }

}
