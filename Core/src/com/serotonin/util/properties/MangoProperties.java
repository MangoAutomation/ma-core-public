/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.util.properties;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;

/**
 * @author Terry Packer
 */
public interface MangoProperties {

    String SYSTEM_ENVIRONMENT_PREFIX = "mango_";
    String SYSTEM_PROPERTIES_PREFIX = "mango.";

    /**
     * Get the raw (un-interpolated) property value
     */
    String getProperty(String key);

    /**
     * Should interpolate the value using the {@link StringSubstitutor} returned by {@link #createInterpolator()}
     */
    String interpolateProperty(String value);

    /**
     * Retrieve the interpolated property value by key.
     */
    default String getString(String key) {
        String value = getProperty(key);
        if (value == null) {
            return null;
        }
        return interpolateProperty(value);
    }

    default String getString(String key, String defaultValue) {
        String value = getString(key);
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        return value;
    }

    default StringSubstitutor createInterpolator() {
        return new StringSubstitutor(this::getProperty)
                .setEnableSubstitutionInVariables(true)
                .setValueDelimiterMatcher(null);
    }

    static Map<String, String> loadFromEnvironment() {
        Map<String, String> values = new HashMap<>();
        for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key.toLowerCase(Locale.ROOT).startsWith(SYSTEM_ENVIRONMENT_PREFIX)) {
                String propertyKey = key.substring(SYSTEM_ENVIRONMENT_PREFIX.length());
                String replacedKey = String.join(".", propertyKey.split("_"));
                values.put(replacedKey, value);
            }
        }
        for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
            if (entry.getKey() instanceof String && entry.getValue() instanceof String) {
                String key = (String) entry.getKey();
                String value = (String) entry.getValue();
                if (key.startsWith(SYSTEM_PROPERTIES_PREFIX)) {
                    values.put(key.substring(SYSTEM_PROPERTIES_PREFIX.length()), value);
                }
            }
        }
        return values;
    }

    static Properties loadFromResources(String resourceName) throws IOException {
        return loadFromResources(resourceName, DefaultMangoProperties.class.getClassLoader());
    }

    static Properties loadFromResources(String resourceName, ClassLoader cl) throws IOException {
        Properties properties = new Properties();
        ArrayList<URL> resources = Collections.list(cl.getResources(resourceName));
        for (URL resource : resources) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.openStream(), StandardCharsets.UTF_8))) {
                properties.load(reader);
            }
        }
        return properties;
    }

    /**
     * Splits on comma and trims results, does not remove empty entries.
     * Returns an empty array if the property is not defined or if it is set to an empty string.
     */
    default String[] getStringArray(String key) {
        return this.getStringArray(key, "\\s*,\\s*", new String[]{});
    }

    /**
     * Splits on comma and trims results, does not remove empty entries.
     * Default is only returned if the property is not defined at all (i.e. commented out).
     * A property defined as an empty string will return an empty array.
     */
    default String[] getStringArray(String key, String[] defaultValue) {
        return this.getStringArray(key, "\\s*,\\s*", defaultValue);
    }

    /**
     * Does not remove empty entries.
     * Default is only returned if the property is not defined at all (i.e. commented out).
     * A property defined as an empty string will return an empty array.
     */
    default String[] getStringArray(String key, String delimiter, String[] defaultValue) {
        String value = getString(key);
        if (value == null)
            return defaultValue;
        if (value.isEmpty()) {
            return new String[]{};
        }
        return value.split(delimiter);
    }

    default int getInt(String key) {
        return Integer.parseInt(getString(key));
    }

    default int getInt(String key, int defaultValue) {
        String value = getString(key);
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }

    default long getLong(String key) {
        return Long.parseLong(getString(key));
    }

    default long getLong(String key, long defaultValue) {
        String value = getString(key);
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        return Long.parseLong(value);
    }

    default boolean getBoolean(String key) {
        return "true".equalsIgnoreCase(getString(key));
    }

    default boolean getBoolean(String key, boolean defaultValue) {
        String value = getString(key);
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(value);
    }

    default double getDouble(String key) {
        return Double.parseDouble(getString(key));
    }

    default double getDouble(String key, double defaultValue) {
        String value = getString(key);
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        return Double.parseDouble(value);
    }

    default String getStringAllowEmpty(String key, String defaultValue) {
        String value = getString(key);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

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
