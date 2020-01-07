/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.util.properties;

import java.util.concurrent.TimeUnit;

/**
 *
 * @author Terry Packer
 */
public interface MangoProperties {

    String getString(String key);

    String getString(String key, String defaultValue);

    default String getStringAllowEmpty(String key, String defaultValue) {
        String value = getString(key);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    /**
     * Does not remove empty entries.
     * Default is only returned if the property is not defined at all (i.e. commented out).
     * A property defined as an empty string will return an empty array.
     */
    String[] getStringArray(String key, String delimiter, String[] defaultValue);

    /**
     * Splits on comma and trims results, does not remove empty entries.
     * Returns an empty array if the property is not defined or if it is set to an empty string.
     */
    String[] getStringArray(String key);

    /**
     * Splits on comma and trims results, does not remove empty entries.
     * Default is only returned if the property is not defined at all (i.e. commented out).
     * A property defined as an empty string will return an empty array.
     */
    String[] getStringArray(String key, String[] defaultValue);

    int getInt(String key);

    int getInt(String key, int defaultValue);

    long getLong(String key);

    long getLong(String key, long defaultValue);

    boolean getBoolean(String key);

    boolean getBoolean(String key, boolean defaultValue);

    double getDouble(String key);

    double getDouble(String key, double defaultValue);

    default TimeUnit getTimeUnitValue(String key, TimeUnit defaultValue) {
        String value = this.getString(key);
        if (value == null) {
            return defaultValue;
        }

        try {
            return TimeUnit.valueOf(key);
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }
}
