/*
 * Copyright (C) 2020 Infinite Automation Systems Inc. All rights reserved.
 */
package com.serotonin.m2m2.db.upgrade;

import java.util.Collections;

/**
 * Adds XID and name columns to file store table
 * @author Jared Wiltshire
 */
public class Upgrade32 extends DBUpgrade {

    @Override
    protected void upgrade() throws Exception {
        runScript(Collections.singletonMap(DEFAULT_DATABASE_TYPE, new String[] {
                "ALTER TABLE fileStores ADD COLUMN xid VARCHAR(100) NOT NULL;",
                "ALTER TABLE fileStores ADD COLUMN name VARCHAR(255) NOT NULL;",
                "UPDATE fileStores SET xid = storeName, name = storeName;",
                "ALTER TABLE fileStores DROP COLUMN storeName;",
                "ALTER TABLE fileStores ADD CONSTRAINT fileStoresUn1 UNIQUE (xid);"
        }));
    }

    @Override
    protected String getNewSchemaVersion() {
        return "33";
    }
}
