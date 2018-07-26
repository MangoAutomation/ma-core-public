/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.rt.event;

import java.util.ArrayList;
import java.util.List;

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
    public void raised(EventInstance evt) {
        this.raised.add(evt);
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.event.UserEventListener#returnToNormal(com.serotonin.m2m2.rt.event.EventInstance)
     */
    @Override
    public void returnToNormal(EventInstance evt) {
        this.returned.add(evt);
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.event.UserEventListener#deactivated(com.serotonin.m2m2.rt.event.EventInstance)
     */
    @Override
    public void deactivated(EventInstance evt) {
        this.deactivated.add(evt);
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.rt.event.UserEventListener#acknowledged(com.serotonin.m2m2.rt.event.EventInstance)
     */
    @Override
    public void acknowledged(EventInstance evt) {
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
    
}