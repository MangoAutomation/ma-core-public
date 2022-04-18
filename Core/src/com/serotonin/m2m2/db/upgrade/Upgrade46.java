/*
 * Copyright (C) 2022 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.upgrade;

import com.infiniteautomation.mango.db.tables.Events;

/**
 * Add composite index to events table to improve query performance
 *
 * @author Terry Packer
 */
public class Upgrade46 extends DBUpgrade {
    @Override
    protected void upgrade() throws Exception {

        Events table = Events.EVENTS;
        create.createIndex("events_performance4")
                .on(table, table.typeName.asc(), table.typeRef1.asc())
                .execute();
    }

    @Override
    protected String getNewSchemaVersion() {
        return "47";
    }
}
