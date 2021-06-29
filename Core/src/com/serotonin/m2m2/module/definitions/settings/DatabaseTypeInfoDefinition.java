/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module.definitions.settings;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.module.SystemInfoDefinition;

/**
 * Class to define Read only settings/information that can be provided
 *
 * @author Terry Packer
 */
public class DatabaseTypeInfoDefinition extends SystemInfoDefinition<String>{

    public final String KEY = "databaseType";

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public String getValue() {
        return Common.databaseProxy.getType().name();
    }

    @Override
    public String getDescriptionKey() {
        return "systemInfo.databaseTypeDesc";
    }

}
