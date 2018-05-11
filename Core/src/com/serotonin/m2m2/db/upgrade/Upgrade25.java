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
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), mysql);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), sql);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), mssql);
        scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), sql);
        runScript(scripts);
    }

    @Override
    protected String getNewSchemaVersion() {
        return "26";
    }
    
    private String[] mysql = new String[]{
    		"ALTER TABLE users ADD COLUMN lastName VARCHAR(255);"
            "CREATE TABLE fileStores (id int not null auto_increment, storeName varchar(100) not null, readPermission varchar(255), writePermission varchar(255), primary key (id)) engine=InnoDB;",
            "ALTER TABLE fileStores ADD CONSTRAINT fileStoresUn1 UNIQUE (storeName);"
    };
    
    private String[] sql = new String[]{
            "ALTER TABLE users ADD COLUMN lastName VARCHAR(255);"
            "CREATE TABLE fileStores (id int not null auto_increment, storeName varchar(100) not null, readPermission varchar(255), writePermission varchar(255), primary key (id));",
            "ALTER TABLE fileStores ADD CONSTRAINT fileStoresUn1 UNIQUE (storeName);"
    };
    
    private String[] mssql = new String[]{
            "ALTER TABLE users ADD COLUMN lastName NVARCHAR(255);"
             "CREATE TABLE fileStores (id int not null identity, storeName nvarchar(100) not null, readPermission nvarchar(255), writePermission nvarchar(255));",
            "ALTER TABLE fileStores ADD CONSTRAINT fileStoresUn1 UNIQUE (storeName);"
    };

}
