/*
 * Copyright (C) 2022 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.upgrade;

import java.io.PrintWriter;

import org.jooq.Field;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

/**
 * Remove data points with dataTypeId = 5 (IMAGE) from the database.
 * If {@link Upgrade19} runs after the IMAGE data type was removed then you will have points with dataTypeId = 0,
 * these will be removed too.
 *
 * @author Jared Wiltshire
 */
public class Upgrade45 extends DBUpgrade {
    @Override
    protected void upgrade() throws Exception {

        try (var log = new PrintWriter(createUpdateLogOutputStream())) {
            log.printf("Deleting all data points with dataTypeId 5 (IMAGE) and 0 (UNKNOWN)%n");
            Field<Integer> dataTypeId = DSL.field("dataTypeId", SQLDataType.INTEGER.nullable(false));
            int deletedPoints = create.deleteFrom(DSL.table("dataPoints"))
                    .where(dataTypeId.in(0, 5))
                    .execute();
            log.printf("Deleted %d data points%n", deletedPoints);

            // there are no other data sources which support IMAGES data points
            log.printf("Deleting all HTTP_IMAGE data sources%n");
            Field<String> dataSourceType = DSL.field("dataSourceType", SQLDataType.VARCHAR(40).nullable(false));
            int deletedSources = create.deleteFrom(DSL.table("dataSources"))
                    .where(dataSourceType.eq("HTTP_IMAGE"))
                    .execute();
            log.printf("Deleted %d HTTP_IMAGE data sources%n", deletedSources);
        }
    }

    @Override
    protected String getNewSchemaVersion() {
        return "46";
    }
}
