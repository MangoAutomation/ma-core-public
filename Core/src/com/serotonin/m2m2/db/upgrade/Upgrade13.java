/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db.upgrade;

import java.util.HashMap;
import java.util.Map;

import com.serotonin.m2m2.db.DatabaseType;

public class Upgrade13 extends DBUpgrade {

    @Override
    public void upgrade() throws Exception {
        // Run the script.
        Map<String, String[]> scripts = new HashMap<>();
        scripts.put(DatabaseType.DERBY.name(), derbyScript);
        scripts.put(DatabaseType.MYSQL.name(), mysqlScript);
        scripts.put(DatabaseType.MSSQL.name(), mssqlScript);
        scripts.put(DatabaseType.H2.name(), h2Script);
        runScript(scripts);
    }

    @Override
    protected String getNewSchemaVersion() {
        return "14";
    }

    private final String[] mssqlScript = {
        "ALTER TABLE users ADD COLUMN name nvarchar(255) NOT NULL DEFAULT '';",
        "UPDATE users SET name='';",
        "ALTER TABLE users ADD COLUMN locale nvarchar(50) NOT NULL DEFAULT '';",
        "UPDATE users SET locale='';"
    };
    private final String[] derbyScript = {
        "ALTER TABLE users ADD COLUMN name varchar(255) NOT NULL DEFAULT '';",
        "UPDATE users SET name='';",
        "ALTER TABLE users ADD COLUMN locale varchar(50) NOT NULL DEFAULT '';",
        "UPDATE users SET locale='';"
    };
    private final String[] mysqlScript = {
        "ALTER TABLE users ADD COLUMN name nvarchar(255) NOT NULL DEFAULT '';",
        "UPDATE users SET name='';",
        "ALTER TABLE users ADD COLUMN locale nvarchar(50) NOT NULL DEFAULT '';",
        "UPDATE users SET locale='';"
    };
    private final String[] h2Script = {
        "ALTER TABLE users ADD COLUMN name varchar(255) NOT NULL DEFAULT '';",
        "UPDATE users SET name='';",
        "ALTER TABLE users ADD COLUMN locale varchar(50) NOT NULL DEFAULT '';",
        "UPDATE users SET locale='';"
    };
}
