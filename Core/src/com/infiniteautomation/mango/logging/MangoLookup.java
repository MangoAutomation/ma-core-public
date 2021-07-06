/*
 * Copyright (C) 2020 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.logging;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.lookup.StrLookup;

import com.serotonin.m2m2.Common;
import com.serotonin.provider.ProviderNotFoundException;
import com.serotonin.provider.Providers;
import com.serotonin.util.properties.MangoProperties;

/**
 * Allows you to access properties from the config file via ${mango:property.name} in log4j2.xml files
 */
@Plugin(name = "mango", category = StrLookup.CATEGORY)
public class MangoLookup implements StrLookup {

    private final MangoProperties mangoProperties;

    public MangoLookup() {
        this.mangoProperties = getProperties();
    }

    private MangoProperties getProperties() {
        try {
            return Providers.get(MangoProperties.class);
        } catch (ProviderNotFoundException e) {
            return null;
        }
    }

    @Override
    public String lookup(String key) {
        if (mangoProperties == null) return null;

        String value = mangoProperties.getString(key);

        // result path values against proper absolute paths
        if (key.equals("paths.home")) {
            return Common.MA_HOME_PATH.toString();
        } else if (key.equals("paths.data")) {
            return Common.MA_DATA_PATH.toString();
        } else if (key.startsWith("paths.")) {
            return Common.MA_DATA_PATH.resolve(value).normalize().toString();
        }

        return value;
    }

    @Override
    public String lookup(LogEvent event, String key) {
        return lookup(key);
    }
}
