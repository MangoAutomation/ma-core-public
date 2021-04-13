/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db.upgrade;

import com.infiniteautomation.mango.db.tables.InstalledModules;

/**
 * Adds the buildTimestamp column to installedModules.
 *
 * @author Jared Wiltshire
 */
public class Upgrade39 extends DBUpgrade {

    @Override
    protected void upgrade() throws Exception {
        InstalledModules im = InstalledModules.INSTALLED_MODULES;

        // allow null values for now
        create.alterTable(im)
                .addColumn(im.buildTimestamp.getName(), im.buildTimestamp.getDataType().nullable(true))
                .execute();

        create.update(im)
                .set(im.buildTimestamp, 0L)
                .execute();

        // set back to non-null
        create.alterTable(im)
                .alterColumn(im.buildTimestamp)
                .set(im.buildTimestamp.getDataType())
                .execute();
    }

    @Override
    protected String getNewSchemaVersion() {
        return "40";
    }
}
