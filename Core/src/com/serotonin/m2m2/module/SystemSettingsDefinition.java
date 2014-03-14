/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.module;

/**
 * This class provides a means to insert a section into the system settings page. Use this if you need to store values
 * in the system settings table that users are allowed to modify.
 * 
 * @author Matthew Lohbihler
 */
abstract public class SystemSettingsDefinition extends ModuleElementDefinition {
    /**
     * The reference key to the description used as the section header.
     * 
     * @return the reference key
     */
    abstract public String getDescriptionKey();

    /**
     * The module relative path to the JSP file that provides the user interface for editing the system settings.
     * 
     * @return the path
     */
    abstract public String getSectionJspPath();
}
