/**
 * @copyright 2018 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.db.upgrade;

import java.util.HashMap;
import java.util.Map;

import com.serotonin.m2m2.db.DatabaseProxy;

/**
 * 3.6.0 Schema Update
 * - Add permissions to mailing lists
 * 
 *
 * @author Terry Packer
 */
public class Upgrade26 extends DBUpgrade {
    
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
        return "27";
    }
    
    private String[] sql = new String[]{
            "ALTER TABLE mailingLists ADD COLUMN readPermission varchar(255);",
            "ALTER TABLE mailingLists ADD COLUMN editPermission varchar(255);"
    };
    
    private String[] mssql = new String[]{
            "ALTER TABLE mailingLists ADD COLUMN readPermission nvarchar(255);",
            "ALTER TABLE mailingLists ADD COLUMN editPermission nvarchar(255);"
    };
}
