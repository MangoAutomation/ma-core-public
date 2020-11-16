/*
 * Copyright (C) 2019 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.rt.event.handlers;

import com.serotonin.m2m2.rt.EventManagerImpl;
import com.serotonin.m2m2.rt.event.EventInstance;

/**
 * @author Jared Wiltshire
 */
public interface EventHandlerInterface {

    /**
     * Not all events that are raised are made active. It depends on the event's alarm level and duplicate handling.
     *
     * @see EventManagerImpl.raiseEvent for details.
     * @param evt
     */
    void eventRaised(EventInstance evt);

    default void eventAcknowledged(EventInstance evt) {}

    /**
     * Called when the event is considered inactive.
     *
     * @see EventManagerImpl.raiseEvent for details.
     * @param evt
     */
    void eventInactive(EventInstance evt);
}
