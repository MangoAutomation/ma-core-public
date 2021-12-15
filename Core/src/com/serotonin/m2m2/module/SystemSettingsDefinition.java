/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.i18n.ProcessResult;

/**
 * This class provides a means to insert a section into the system settings page. Use this if you need to store values
 * in the system settings table that users are allowed to modify.
 *
 * @author Matthew Lohbihler, Terry Packer
 */
abstract public class SystemSettingsDefinition extends ModuleElementDefinition {

    @Autowired
    protected SystemSettingsDao systemSettingsDao;

    public static Integer getIntegerValue(Object value) {
        if(value == null)
            return null;
        if(value instanceof Number)
            return ((Number)value).intValue();
        if(value instanceof String)
            return Integer.parseInt((String)value);
        if(value instanceof Integer)
            return (Integer)value;
        return null;
    }

    public static Double getDoubleValue(Object value) {
        if(value == null)
            return null;
        if(value instanceof Number)
            return ((Number)value).doubleValue();
        if(value instanceof String)
            return Double.parseDouble((String)value);
        if(value instanceof Double)
            return (Double)value;
        return null;
    }
    /**
     * The reference key to the description used as the section header.
     *
     * @return the reference key
     */
    abstract public String getDescriptionKey();

    /**
     * Get the default values for the defined system settings
     */
    abstract public Map<String, Object> getDefaultValues();

    /**
     * Potentially convert a value from a code to its integer value
     * @param key - Key of setting
     * @param code - String export code value
     * @return Intger if convertable, else null
     */
    abstract public Integer convertToValueFromCode(String key, String code);

    /**
     * @param key - Key of setting
     * @param value - Integer value for code
     * @return String export code
     */
    abstract public String convertToCodeFromValue(String key, Integer value);

    /**
     * <p>Validate the settings.  The general idea is to use the Settings key as the contextual key when generating error messages.</p>
     *
     * <p>This method is called every time any system setting is saved. The settings you are interested in may not be present in the map.</p>
     *
     * <p>Strings representing ExportCodes will have been converted to their integer values prior to this method being called. </p>
     *
     * <p>The value of a JSON system setting will be either a JsonNode (if sent via REST) or a String (if imported via configuration import).
     * You might want to use a SystemSettingsDao method such as {@link com.serotonin.m2m2.db.dao.SystemSettingsDao#readAsJson(Object, Class)}
     * to validate that the JsonNode or String can be converted to the desired class.</p>
     *
     * <p>Values may be present in the map as Integers (if set via REST as a number) or BigDecimals (if imported via configuration import as a number).</p>
     *
     * <p>A value of null indicates that the key is to be removed from the system settings table.</p>
     *
     */
    abstract public void validateSettings(Map<String, Object> settings, ProcessResult response);
}
