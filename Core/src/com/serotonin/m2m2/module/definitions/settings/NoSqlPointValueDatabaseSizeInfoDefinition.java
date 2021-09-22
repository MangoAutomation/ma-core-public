/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module.definitions.settings;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.serotonin.m2m2.db.PointValueDaoDefinition;
import com.serotonin.m2m2.module.SystemInfoDefinition;

/**
 * Class to define Read only settings/information that can be provided
 *
 * @author Terry Packer
 */
public class NoSqlPointValueDatabaseSizeInfoDefinition extends SystemInfoDefinition<Long>{

    public final String KEY = "noSqlPointValueDatabaseSize";

    @Autowired
    private List<PointValueDaoDefinition> definitions;

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public Long getValue() {
        long noSqlSize = 0L;
        try {
            noSqlSize = definitions.stream().findFirst().orElseThrow().getDatabaseSizeInBytes();
        } catch (UnsupportedOperationException e) {
            // ignore
        }
        return noSqlSize;
    }

    @Override
    public String getDescriptionKey() {
        return "systemInfo.noSqlDatabaseSizeDesc";
    }

}
