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
public class SqlDatabaseSizeInfoDefinition extends SystemInfoDefinition<Long>{

    public final String KEY = "sqlDatabaseSize";

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public Long getValue() {
        // Database size
        try {
            return Common.databaseProxy.getDatabaseSizeInBytes();
        } catch (UnsupportedOperationException e) {
            return null;
        }
    }

    @Override
    public String getDescriptionKey() {
        return "systemInfo.databaseSizeDesc";
    }

}
