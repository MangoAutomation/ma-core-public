/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring.events;

import org.eclipse.jetty.server.session.AbstractSessionDataStore;
import org.springframework.context.ApplicationEvent;

import com.infiniteautomation.mango.spring.eventMulticaster.PropagatingEvent;
import com.serotonin.m2m2.vo.User;

/**
 * Event when a persistent session is loaded from a store
 *  this session may not be valid as it could be expired, but
 *  it was found in the store.
 *
 *  If the session is expired it will be invalidated by Jetty
 *   and not used.
 *
 *   The sessionId and principle will never be null
 *
 * @author Terry Packer
 */
public class SessionLoadedEvent extends ApplicationEvent implements PropagatingEvent{

    private static final long serialVersionUID = 1L;

    private final String sessionId;
    private final User principle;

    public SessionLoadedEvent(AbstractSessionDataStore source, String sessionId, User principle) {
        super(source);
        this.sessionId = sessionId;
        this.principle = principle;
    }

    public String getSessionId() {
        return sessionId;
    }

    public User getPrinciple() {
        return principle;
    }

}
