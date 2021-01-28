/**
 * @copyright 2018 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Phillip Dunlap
 */
package com.serotonin.m2m2.db.upgrade;

import java.util.HashMap;
import java.util.Map;

import com.serotonin.m2m2.db.DatabaseType;

public class Upgrade24 extends DBUpgrade {

    @Override
    protected void upgrade() throws Exception {
        Map<String, String[]> scripts = new HashMap<>();
        scripts.put(DatabaseType.MYSQL.name(), sql);
        scripts.put(DatabaseType.H2.name(), sql);
        scripts.put(DatabaseType.MSSQL.name(), mssql);
        scripts.put(DatabaseType.POSTGRES.name(), sql);
        runScript(scripts);
    }

    @Override
    protected String getNewSchemaVersion() {
        return "25";
    }
    
    private String[] sql = new String[]{
            "CREATE TABLE installedModules (name varchar(30) not null, version varchar(255) not null);",
            "ALTER TABLE installedModules ADD CONSTRAINT installModulesUn1 UNIQUE (name);"
    };
    
    private String[] mssql = new String[]{
            "CREATE TABLE installedModules (name nvarchar(30) not null, version nvarchar(255) not null);",
            "ALTER TABLE installedModules ADD CONSTRAINT installModulesUn1 UNIQUE (name);"
    };

}
