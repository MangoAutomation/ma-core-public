/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.db.upgrade;

import java.util.HashMap;
import java.util.Map;

import com.serotonin.m2m2.db.DatabaseProxy;

/**
 * Add organization column to user's table
 * 
 * @author Terry Packer
 *
 */
public class Upgrade28 extends DBUpgrade {
    
    
    @Override
    protected void upgrade() throws Exception {
        
        Map<String, String[]> scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), sql);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), sql);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), mssql);
        scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), sql);
        runScript(scripts);

    }
    
    private String[] sql = new String[]{
            "ALTER TABLE users ADD COLUMN organization varchar(80);",
    };
    private String[] mssql = new String[]{
            "ALTER TABLE users ADD COLUMN organization varchar(80);",
    };

    @Override
    protected String getNewSchemaVersion() {
        return "29";
    }
}
