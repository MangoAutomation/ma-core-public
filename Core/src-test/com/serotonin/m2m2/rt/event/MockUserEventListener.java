/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.vo.User;

/**
 *
 * @author Terry Packer
 */
public class MockUserEventListener implements UserEventListener {

    protected User user;
    protected List<EventInstance> raised = new ArrayList<>();
    protected List<EventInstance> returned = new ArrayList<>();
    protected List<EventInstance> deactivated = new ArrayList<>();
    protected List<EventInstance> acknowledged = new ArrayList<>();
    protected List<Class<? extends EventType>> eventsToListenFor;
    
    public MockUserEventListener(User user, Class<? extends EventType> type) {
        this.user = user;
        this.eventsToListenFor = Arrays.asList(type);
    }
    
    public MockUserEventListener(User user, List<Class<? extends EventType>> types) {
        this.user = user;
        this.eventsToListenFor = types;
    }
    
    public MockUserEventListener(User user) {
        this.user = user;
    }
    
    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.event.UserEventListener#getUserId()
     */
    @Override
    public int getUserId() {
        return user.getId();
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.event.UserEventListener#raised(com.serotonin.m2m2.rt.event.EventInstance)
     */
    @Override
    public synchronized void raised(EventInstance evt) {
        if(!isListening(evt))
            return;
        this.raised.add(evt);
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.event.UserEventListener#returnToNormal(com.serotonin.m2m2.rt.event.EventInstance)
     */
    @Override
    public synchronized void returnToNormal(EventInstance evt) {
        if(!isListening(evt))
            return;
        this.returned.add(evt);
    }

    @Override
    public synchronized void deactivated(EventInstance evt) {
        if(!isListening(evt))
            return;
        this.deactivated.add(evt);
    }

    @Override
    public synchronized void acknowledged(EventInstance evt) {
        if(!isListening(evt))
            return;
        this.acknowledged.add(evt);
    }

    public User getUser() {
        return user;
    }

    public List<EventInstance> getRaised() {
        return raised;
    }

    public List<EventInstance> getReturned() {
        return returned;
    }

    public List<EventInstance> getDeactivated() {
        return deactivated;
    }

    public List<EventInstance> getAcknowledged() {
        return acknowledged;
    }
    
    private boolean isListening(EventInstance evt) {
        if(eventsToListenFor == null)
            return true;
        for(Class<? extends EventType> t : eventsToListenFor)
            if(evt.getEventType().getClass() == t)
                return true;
        return false;
    }
    
}