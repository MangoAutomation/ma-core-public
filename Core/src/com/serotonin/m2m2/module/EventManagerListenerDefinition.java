/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.EventInstance;
import com.serotonin.m2m2.rt.event.type.EventType;

/**
 * Provides a hook for modules to "listen" for things that happen in the event manager.
 *
 * @author Matthew Lohbihler
 * @author Jared Wiltshire
 */
abstract public class EventManagerListenerDefinition extends ModuleElementDefinition {
    /**
     * Provides both notification of a new event, and the opportunity to automatically acknowledge it.
     *
     * @param event the event being raised.
     * @return the message with which to acknowledge the event, or null if it should not be acknowledged.
     */
    public TranslatableMessage autoAckEventWithMessage(EventInstance event) {
        return null;
    }


    /**
     * Provides a hook where the event instance can be modified by returning a new instance.
     * Can also be used to drop or ignore an event.
     *
     * This runs after the duplicate/recent checks but before the event is saved to the database.
     *
     * @param event the event being raised.
     * @return the new event or null to ignore/drop the event completely
     */
    public EventInstance modifyEvent(EventInstance event) {
        return event;
    }

}
