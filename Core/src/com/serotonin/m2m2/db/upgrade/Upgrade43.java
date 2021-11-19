/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db.upgrade;

import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

/**
 * Add the timeSeriesMigrationProgress table
 */
public class Upgrade43 extends DBUpgrade {

    @Override
    protected void upgrade() throws Exception {
        create.createTable("timeSeriesMigrationProgress")
                .column("seriesId", SQLDataType.INTEGER.nullable(false))
                .column("status", SQLDataType.VARCHAR(100).nullable(false))
                .column("timestamp", SQLDataType.BIGINT.nullable(false))
                .constraints(
                        DSL.constraint("timeSeriesMigrationProgressFk1")
                            .foreignKey("seriesId")
                            .references("timeSeries", "id")
                            .onDeleteCascade(),
                        DSL.constraint("timeSeriesMigrationProgress").unique("seriesId")
                ).execute();
    }

    @Override
    protected String getNewSchemaVersion() {
        return "44";
    }
}
