/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.module;

import java.util.List;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.vo.event.EventTypeVO;

/**
 * Used for creating custom event types.
 */
abstract public class EventTypeDefinition extends ModuleElementDefinition {
    /**
     * The type name of the system event. Must be unique within the instance, and less than or equal to 30 characters in
     * length.
     */
    abstract public String getTypeName();

    /**
     * The class that represents the event type.
     * 
     * @return the event type class
     */
    abstract public Class<? extends EventType> getEventTypeClass();

    /**
     * Create an instance of the event type using the given parameters.
     * 
     * @param subtype
     *            the subtype of the event. May be null
     * @param ref1
     *            the first reference id. Event types define how this number is used
     * @param ref2
     *            the second reference id. Event types define how this number is used
     * @return the new EventType object.
     */
    abstract public EventType createEventType(String subtype, int ref1, int ref2);

    /**
     * Whether admin permission is required to create and edit event handlers for this even type.
     * 
     * @return true if admin permission is required.
     */
    abstract public boolean getHandlersRequireAdmin();

    /**
     * Returns a list of EventTypeVOs representing the list of events to which handlers can be added/edited.
     * 
     * @return the list of event type VO objects.
     */
    abstract public List<EventTypeVO> getEventTypeVOs();

    /**
     * Optional. The module relative path to an icon that represents the event type.
     * 
     * @return the path to the icon, or null if none.
     */
    abstract public String getIconPath();

    /**
     * The reference key to the description of the event type.
     * 
     * @return the description key
     */
    abstract public String getDescriptionKey();

    /**
     * Optional. For use on the event list page, this provides a link from an event instance back to the object that
     * raised it.
     * 
     * @param subtype
     *            the event subtype
     * @param ref1
     *            the first reference id
     * @param ref2
     *            the second reference id
     * @param translations
     *            the translations object for the current user
     * @return the link to display, or null if none
     */
    abstract public String getEventListLink(String subtype, int ref1, int ref2, Translations translations);

    /**
     * Optional. The message to display if the object that raised an event instance is disabled, causing the event to be
     * deactivated.
     * 
     * @return the translatable message instance, or null.
     */
    abstract public TranslatableMessage getSourceDisabledMessage();

}
