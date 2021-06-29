/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module;

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

}
