/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.util.properties;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;

/**
 * @author Jared Wiltshire
 */
public class ReloadingProperties implements MangoProperties {

    protected final Properties properties;

    public ReloadingProperties(Properties properties) {
        this.properties = properties;
    }

    @Override
    public String getString(String key) {
        return properties.getProperty(key);
    }

    public static Properties loadFromResources(String resourceName, ClassLoader cl) throws IOException {
        Properties properties = new Properties();
        ArrayList<URL> resources = Collections.list(cl.getResources("env.properties"));
        for (URL resource : resources) {
            try (InputStream is = resource.openStream()) {
                properties.load(is);
            }
        }
        return properties;
    }
}
