/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Properties;

import com.serotonin.m2m2.vo.User;
import com.serotonin.util.properties.DefaultMangoProperties;
import com.serotonin.util.properties.MangoProperties;

/**
 * Dummy implementation of properties for use in testing.
 *
 * @author Terry Packer
 */
public class MockMangoProperties implements MangoProperties {

    private final Properties properties;

    public MockMangoProperties() {
        try {
            this.properties = DefaultMangoProperties.loadFromResources("env.properties");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        // change/set properties for testing
        setProperty("web.openBrowserOnStartup", "false");

        //Test injection property types
        setProperty("test.injectedStringArray", "ONE,TWO,THREE");
        setProperty("test.injectedIntegerArray", "1,2,3");
        setProperty("test.injectedBoolean","true");
        setProperty("test.injectedString","Testing String");
        setProperty("test.injectedInteger", "1");
        setProperty("test.injectedEmptyStringArray", "");

        //To avoid long delays when testing serial ports
        setProperty("serial.port.linux.regex", "null");
        setProperty("serial.port.linux.path", "/dev/");
        setProperty("serial.port.osx.regex", "null");
        setProperty("serial.port.osx.path", "/dev/");
    }

    public void setProperty(String key, String value) {
        this.properties.setProperty(key, value);
    }

    @Override
    public String getString(String key) {
        String value = System.getProperty(key);
        if (value == null) {
            value = properties.getProperty(key);
        }
        return interpolateProperty(value);
    }
}
