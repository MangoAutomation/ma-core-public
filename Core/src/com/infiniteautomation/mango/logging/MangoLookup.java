/*
 * Copyright (C) 2020 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.logging;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.lookup.StrLookup;

import com.serotonin.provider.Providers;
import com.serotonin.util.properties.MangoProperties;

/**
 * Allows you to access env.properties via ${mango:property.name} in log4j2.xml files
 */
@Plugin(name = "mango", category = StrLookup.CATEGORY)
public class MangoLookup implements StrLookup {

    private final MangoProperties mangoProperties;

    public MangoLookup() {
        this.mangoProperties = Providers.get(MangoProperties.class);
    }

    @Override
    public String lookup(String key) {
        return mangoProperties.getString(key);
    }

    @Override
    public String lookup(LogEvent event, String key) {
        return lookup(key);
    }
}
