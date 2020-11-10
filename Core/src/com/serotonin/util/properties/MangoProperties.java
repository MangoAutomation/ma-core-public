/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.util.properties;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Terry Packer
 */
public interface MangoProperties {

    static final Pattern INTERPOLATION_PATTERN = Pattern.compile("\\$\\{(.+?)\\}");

    public String getString(String key);

    public default String interpolateProperty(String value) {
        if (value == null) return value;
        Matcher matcher = INTERPOLATION_PATTERN.matcher(value);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String interpolatedKey = matcher.group(1);
            String interpolatedValue = getString(interpolatedKey);
            if (interpolatedValue == null) {
                throw new IllegalStateException("Property has no value: " + interpolatedKey);
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(interpolatedValue));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    public default String getString(String key, String defaultValue) {
        String value = getString(key);
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        return value;
    }

    /**
     * Splits on comma and trims results, does not remove empty entries.
     * Returns an empty array if the property is not defined or if it is set to an empty string.
     */
    public default String[] getStringArray(String key) {
        return this.getStringArray(key, "\\s*,\\s*", new String[] {});
    }

    /**
     * Splits on comma and trims results, does not remove empty entries.
     * Default is only returned if the property is not defined at all (i.e. commented out).
     * A property defined as an empty string will return an empty array.
     */
    public default String[] getStringArray(String key, String[] defaultValue) {
        return this.getStringArray(key, "\\s*,\\s*", defaultValue);
    }

    /**
     * Does not remove empty entries.
     * Default is only returned if the property is not defined at all (i.e. commented out).
     * A property defined as an empty string will return an empty array.
     */
    public default String[] getStringArray(String key, String delimiter, String[] defaultValue) {
        String value = getString(key);
        if (value == null)
            return defaultValue;
        if (value.isEmpty()) {
            return new String[] {};
        }
        return value.split(delimiter);
    }

    public default int getInt(String key) {
        return Integer.parseInt(getString(key));
    }

    public default int getInt(String key, int defaultValue) {
        String value = getString(key);
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }

    public default long getLong(String key) {
        return Long.parseLong(getString(key));
    }

    public default long getLong(String key, long defaultValue) {
        String value = getString(key);
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        return Long.parseLong(value);
    }

    public default boolean getBoolean(String key) {
        return "true".equalsIgnoreCase(getString(key));
    }

    public default boolean getBoolean(String key, boolean defaultValue) {
        String value = getString(key);
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(value);
    }

    public default double getDouble(String key) {
        return Double.parseDouble(getString(key));
    }

    public default double getDouble(String key, double defaultValue) {
        String value = getString(key);
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        return Double.parseDouble(value);
    }

    public default String getStringAllowEmpty(String key, String defaultValue) {
        String value = getString(key);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    public default TimeUnit getTimeUnitValue(String key, TimeUnit defaultValue) {
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
