/**
 * @copyright 2018 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.db.upgrade;

import java.util.HashMap;
import java.util.Map;

import com.serotonin.m2m2.db.DatabaseProxy;

/**
 * 3.5.0 Schema Update
 * - Add lastName column to Users table
 * 
 *
 * @author Terry Packer
 */
public class Upgrade25 extends DBUpgrade {

    @Override
    protected void upgrade() throws Exception {
        Map<String, String[]> scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), sql);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), sql);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), mssql);
        scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), sql);
        runScript(scripts);
    }

    @Override
    protected String getNewSchemaVersion() {
        return "26";
    }
    
    private String[] sql = new String[]{
            "ALTER TABLE users ADD COLUMN lastName VARCHAR(255);"
    };
    
    private String[] mssql = new String[]{
            "ALTER TABLE users ADD COLUMN lastName NVARCHAR(255);"
    };

}
