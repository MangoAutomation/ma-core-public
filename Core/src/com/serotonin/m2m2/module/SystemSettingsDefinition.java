/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.module;

import java.util.Map;

import com.serotonin.m2m2.i18n.ProcessResult;

/**
 * This class provides a means to insert a section into the system settings page. Use this if you need to store values
 * in the system settings table that users are allowed to modify.
 * 
 * @author Matthew Lohbihler
 */
abstract public class SystemSettingsDefinition extends ModuleElementDefinition {
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
     * The module relative path to the JSP file that provides the user interface for editing the system settings.
     * 
     * @return the path
     */
    abstract public String getSectionJspPath();
    
    /**
     * Get the default values for the defined system settings
     * @return
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
	 * Validate the settings.  The general idea is to use the Settings key as the contextual key when generating error messages
	 * 
	 * Note: The setting(s) may not be present in the map, which should not invalidate the response.
	 * 
	 * @param settings
	 * @param response
	 */
	abstract public void validateSettings(Map<String, Object> settings, ProcessResult response);
}
