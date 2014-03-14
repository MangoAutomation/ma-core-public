/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.module;

import com.serotonin.m2m2.i18n.Translations;

/**
 * Used for creating custom audit event types.
 */
abstract public class AuditEventTypeDefinition extends ModuleElementDefinition {
    /**
     * The type name of the audit event. Must be unique within the instance, and should be less than 30 characters.
     */
    abstract public String getTypeName();

    /**
     * A reference to a human readable and translatable name of the audit event type. Key reference values in
     * i18n.properties files.
     * 
     * @return the reference key to the audit event short description.
     */
    abstract public String getDescriptionKey();

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
}
