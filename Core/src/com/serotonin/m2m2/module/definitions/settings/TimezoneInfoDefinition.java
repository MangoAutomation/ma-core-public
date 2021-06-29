/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module.definitions.settings;

import java.util.TimeZone;

import com.serotonin.m2m2.module.SystemInfoDefinition;

/**
 * Class to define Read only settings/information that can be provided
 *
 * @author Terry Packer
 */
public class TimezoneInfoDefinition extends SystemInfoDefinition<String>{

    public final String KEY = "timezone";

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public String getValue() {
        return TimeZone.getDefault().getID();
    }

    @Override
    public String getDescriptionKey() {
        return "systemInfo.timezoneDesc";
    }

}
