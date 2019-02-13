/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.module;

import java.util.Collections;
import java.util.List;

import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.type.SystemEventType;

/**
 * Used for creating custom system event types.
 */
abstract public class SystemEventTypeDefinition extends ModuleElementDefinition {
    /**
     * The type name of the system event. Must be unique within the instance, and should be less than 30 characters.
     */
    abstract public String getTypeName();

    /**
     * A reference to a human readable and translatable name of the system event type. Key reference values in
     * i18n.properties files.
     *
     * @return the reference key to the system event short description.
     */
    abstract public String getDescriptionKey();

    /**
     * The default alarm level of the event. Defaults to AlarmLevels.URGENT. Override as necessary, but note that users
     * can override within the instance.
     */
    public AlarmLevels getDefaultAlarmLevel() {
        return AlarmLevels.URGENT;
    }

    /**
     * This method provides an opportunity for the client code to return HTML code that will be used verbatim in the
     * events list snippet.
     *
     * @param ref1
     *            the first reference id of the event
     * @param ref2
     *            the second reference id of the event
     * @param translations
     *            the translations object with which to translate translatable messages
     * @return the link, or null if none is to be displayed.
     */
    abstract public String getEventListLink(int ref1, int ref2, Translations translations);
    
    /**
     * Does this event type use typeref1?
     * @return
     */
    abstract public boolean supportsReferenceId1();
    
    /**
     * Does this event type use typeref2?
     * @return
     */
    abstract public boolean supportsReferenceId2();
    
    /**
     * If this event type support reference id 1, return a list of the possible event types 
     * for all the reference id 1s
     * @return
     */
    public List<SystemEventType> genegeneratePossibleEventTypesWithReferenceId1() { return Collections.emptyList(); }
}
