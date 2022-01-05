/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db.upgrade;

import com.infiniteautomation.mango.db.tables.DataSources;

/**
 * Add composite index to data sources table to improve data point join query performance
 */
public class Upgrade44 extends DBUpgrade {

    @Override
    protected void upgrade() throws Exception {
        DataSources table = DataSources.DATA_SOURCES;
        create.createIndex("dataSourcesIdNameTypeXidIndex")
                .on(table, table.id.asc(), table.name.asc(), table.dataSourceType.asc(), table.xid.asc())
                .execute();
    }

    @Override
    protected String getNewSchemaVersion() {
        return "45";
    }
}
