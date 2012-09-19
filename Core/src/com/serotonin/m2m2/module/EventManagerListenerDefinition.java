/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.module;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.type.EventType;

/**
 * Provides a hook for modules to "listen" for things that happen in the event manager.
 * 
 * @author Matthew Lohbihler
 */
abstract public class EventManagerListenerDefinition extends ModuleElementDefinition {
    /**
     * Provides both notification of a new event, and the opportunity to automatically acknowledge it.
     * 
     * @param eventType
     *            the type of event being raised.
     * @return the message with which to acknowledge the event, or null if it should not be acknowledged.
     */
    abstract public TranslatableMessage autoAckEventWithMessage(EventType eventType);

}
