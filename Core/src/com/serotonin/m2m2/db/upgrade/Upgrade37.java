/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db.upgrade;

import com.infiniteautomation.mango.db.tables.InstalledModules;
import com.serotonin.m2m2.Common;

/**
 * Adds the upgradedTimestamp column to installedModules.
 *
 * @author Jared Wiltshire
 */
public class Upgrade37 extends DBUpgrade {

    @Override
    protected void upgrade() throws Exception {
        InstalledModules im = InstalledModules.INSTALLED_MODULES;

        // allow null values for now
        create.alterTable(im)
                .addColumn(im.upgradedTimestamp.getName(), im.upgradedTimestamp.getDataType().nullable(true))
                .execute();

        create.update(im)
                .set(im.upgradedTimestamp, Common.START_TIME)
                .execute();

        // set back to non-null
        create.alterTable(im)
                .alterColumn(im.upgradedTimestamp)
                .set(im.upgradedTimestamp.getDataType())
                .execute();
    }

    @Override
    protected String getNewSchemaVersion() {
        return "38";
    }
}
