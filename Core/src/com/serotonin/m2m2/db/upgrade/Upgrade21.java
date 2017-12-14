/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.db.upgrade;

import java.util.HashMap;
import java.util.Map;

import com.serotonin.m2m2.db.DatabaseProxy;

/**
 * @author Jared Wiltshire
 */
public class Upgrade21 extends DBUpgrade {

    @Override
    protected void upgrade() throws Exception {
        Map<String, String[]> scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), mysql);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), h2);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), mssql);
        scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), postgres);
        runScript(scripts);
    }

    @Override
    protected String getNewSchemaVersion() {
        return "22";
    }
    
    private final String[] mysql = new String[] {
            "ALTER TABLE users ADD COLUMN tokenVersion INT;",
            "ALTER TABLE users ADD COLUMN passwordVersion INT;",
            "UPDATE users SET tokenVersion = 1;",
            "UPDATE users SET passwordVersion = 1;",
            "ALTER TABLE users MODIFY COLUMN tokenVersion INT NOT NULL;",
            "ALTER TABLE users MODIFY COLUMN passwordVersion INT NOT NULL;"
    };
    
    private final String[] h2 = new String[] {
            "ALTER TABLE users ADD COLUMN tokenVersion INT;",
            "ALTER TABLE users ADD COLUMN passwordVersion INT;",
            "UPDATE users SET tokenVersion = 1;",
            "UPDATE users SET passwordVersion = 1;",
            "ALTER TABLE users ALTER COLUMN tokenVersion INT NOT NULL;",
            "ALTER TABLE users ALTER COLUMN passwordVersion INT NOT NULL;"
    };
    
    private final String[] mssql = new String[] {
            "ALTER TABLE users ADD COLUMN tokenVersion INT;",
            "ALTER TABLE users ADD COLUMN passwordVersion INT;",
            "UPDATE users SET tokenVersion = 1;",
            "UPDATE users SET passwordVersion = 1;",
            "ALTER TABLE users ALTER COLUMN tokenVersion INT NOT NULL;",
            "ALTER TABLE users ALTER COLUMN passwordVersion INT NOT NULL;"
    };
    
    private final String[] postgres = new String[] {
            "ALTER TABLE users ADD COLUMN tokenVersion INT;",
            "ALTER TABLE users ADD COLUMN passwordVersion INT;",
            "UPDATE users SET tokenVersion = 1;",
            "UPDATE users SET passwordVersion = 1;",
            "ALTER TABLE users ALTER COLUMN tokenVersion INT NOT NULL;",
            "ALTER TABLE users ALTER COLUMN passwordVersion INT NOT NULL;"
    };
}
