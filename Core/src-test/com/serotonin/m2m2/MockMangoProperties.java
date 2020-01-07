/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2;

import java.util.Properties;

import com.serotonin.m2m2.vo.User;
import com.serotonin.util.properties.ReloadingProperties;

/**
 * Dummy implementation of properties for use in testing.
 *
 * @author Terry Packer
 */
public class MockMangoProperties extends ReloadingProperties {

    public MockMangoProperties() {
        this(new Properties());
    }

    public MockMangoProperties(Properties properties) {
        super(properties);

        setProperty("web.openBrowserOnStartup", "false");

        //Fill in all default values for properties
        setProperty("db.update.log.dir", Common.MA_HOME + "/logs/");
        setProperty("security.hashAlgorithm", User.BCRYPT_ALGORITHM);

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
}
