/*
 * Copyright (C) 2020 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.logging;

import org.apache.logging.log4j.util.PropertySource;

import com.serotonin.provider.Providers;
import com.serotonin.util.properties.MangoProperties;

/**
 * Allows configuring <a href="https://logging.apache.org/log4j/2.x/manual/configuration.html#System_Properties"> log4j properties</a>
 * via env.properties file
 */
public class MangoPropertySource implements PropertySource {

    private final MangoProperties mangoProperties;

    public MangoPropertySource() {
        this.mangoProperties = Providers.get(MangoProperties.class);
    }

    @Override
    public int getPriority() {
        // after all the default log4j mechanisms
        return 110;
    }

    @Override
    public String getProperty(String key) {
        return getPropertyInternal(key);
    }

    @Override
    public boolean containsProperty(String key) {
        return getPropertyInternal(key) != null;
    }

    private String getPropertyInternal(String key) {
        // only support properties starting with log4j2 to prevent collision
        return key.startsWith("log4j2.") ? this.mangoProperties.getString(key) : null;
    }
}
