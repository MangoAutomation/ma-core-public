/*
 * Copyright (C) 2020 Infinite Automation Systems Inc. All rights reserved.
 */
package com.serotonin.m2m2.db.upgrade;

import java.util.HashMap;
import java.util.Map;

import com.serotonin.m2m2.db.DatabaseType;

/**
 * Drops redundant indexes
 * @author Jared Wiltshire
 */
public class Upgrade31 extends DBUpgrade {
    @Override
    protected void upgrade() throws Exception {
        Map<String, String[]> scripts = new HashMap<>();
        scripts.put(DEFAULT_DATABASE_TYPE, new String[0]);
        scripts.put(DatabaseType.MYSQL.name(), new String[] {
                "ALTER TABLE dataPoints DROP INDEX deviceNameNameIndex;",
                "ALTER TABLE dataPoints DROP INDEX deviceNameIndex;"
        });
        scripts.put(DatabaseType.MSSQL.name(), new String[] {
                "DROP INDEX dataPoints.deviceNameNameIndex;",
                "DROP INDEX dataPoints.deviceNameIndex;"
        });
        // H2 does not create the redundant indexes
        runScript(scripts);
    }

    @Override
    protected String getNewSchemaVersion() {
        return "32";
    }
}
