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

    String getDescription();

    String getString(String key);

    String getString(String key, String defaultValue);

    String[] getStringArray(String key, String delimiter, String[] defaultValue);

    int getInt(String key);

    int getInt(String key, int defaultValue);

    long getLong(String key);

    long getLong(String key, long defaultValue);

    boolean getBoolean(String key);

    boolean getBoolean(String key, boolean defaultValue);

    double getDouble(String key);

    double getDouble(String key, double defaultValue);

    void setDefaultValue(String key, String value);

    // TODO Mango 3.5 remove Deprecated
    // Dont use in modules for now
    @Deprecated
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
