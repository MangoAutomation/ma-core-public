/**
 * @copyright 2018 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.db.upgrade;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.DatabaseProxy;

/**
 * 3.5.0 Schema Update
 * - Add lastName column to Users table
 *
 *
 * @author Terry Packer
 */
public class Upgrade25 extends DBUpgrade {
    private final Log LOG = LogFactory.getLog(Upgrade25.class);

    @Override
    protected void upgrade() throws Exception {
        Map<String, String[]> scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), mysql);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), sql);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), mssql);
        scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), sql);
        runScript(scripts);

        //Set all the timestamps for the password change to now
        long now = Common.timer.currentTimeMillis();
        this.ejt.update("UPDATE users SET passwordChangeTimestamp=?", now);

        //Alter the columns to be non null
        scripts.clear();
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), mysqlAlterUserPasswordChangeTimestamp);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), sqlAlterUserPasswordChangeTimestamp);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), sqlAlterUserPasswordChangeTimestamp);
        scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), sqlAlterUserPasswordChangeTimestamp);
        runScript(scripts);

        File swaggerDirectory = Common.WEB.resolve("swagger").toFile();
        try {
            FileUtils.deleteDirectory(swaggerDirectory);
        } catch(Exception e) {
            LOG.error("Error deleting V1 swagger directory: " + e.getMessage(), e);
        }
    }

    @Override
    protected String getNewSchemaVersion() {
        return "26";
    }

    private String[] mysql = new String[]{
            "CREATE TABLE fileStores (id int not null auto_increment, storeName varchar(100) not null, readPermission varchar(255), writePermission varchar(255), primary key (id)) engine=InnoDB;",
            "ALTER TABLE fileStores ADD CONSTRAINT fileStoresUn1 UNIQUE (storeName);",
            "ALTER TABLE users ADD COLUMN passwordChangeTimestamp BIGINT;"
    };

    private String[] sql = new String[]{
            "CREATE TABLE fileStores (id int not null auto_increment, storeName varchar(100) not null, readPermission varchar(255), writePermission varchar(255), primary key (id));",
            "ALTER TABLE fileStores ADD CONSTRAINT fileStoresUn1 UNIQUE (storeName);",
            "ALTER TABLE users ADD COLUMN passwordChangeTimestamp BIGINT;"
    };

    private String[] mssql = new String[]{
            "CREATE TABLE fileStores (id int not null identity, storeName nvarchar(100) not null, readPermission nvarchar(255), writePermission nvarchar(255));",
            "ALTER TABLE fileStores ADD CONSTRAINT fileStoresUn1 UNIQUE (storeName);",
            "ALTER TABLE users ADD COLUMN passwordChangeTimestamp BIGINT;"
    };

    private String[] mysqlAlterUserPasswordChangeTimestamp = new String[] {
            "ALTER TABLE users MODIFY COLUMN passwordChangeTimestamp BIGINT NOT NULL;"
    };
    private String[] sqlAlterUserPasswordChangeTimestamp = new String[] {
            "ALTER TABLE users ALTER COLUMN passwordChangeTimestamp BIGINT NOT NULL;"
    };
}
