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
public class NoSqlPointValueDatabaseSizeInfoDefinition extends SystemInfoDefinition<Long>{

    public final String KEY = "noSqlPointValueDatabaseSize";

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public Long getValue() {
        long noSqlSize = 0L;
        if (Common.databaseProxy.getNoSQLProxy() != null) {
            String pointValueStoreName = Common.envProps.getString("db.nosql.pointValueStoreName", "mangoTSDB");
            noSqlSize = Common.databaseProxy.getNoSQLProxy().getDatabaseSizeInBytes(pointValueStoreName);
        }
        return noSqlSize;
    }

    @Override
    public String getDescriptionKey() {
        return "systemInfo.noSqlDatabaseSizeDesc";
    }

}
