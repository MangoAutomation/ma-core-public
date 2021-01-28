package com.serotonin.m2m2.db.upgrade;

import java.util.HashMap;
import java.util.Map;

import com.serotonin.m2m2.db.DatabaseType;

public class Upgrade23 extends DBUpgrade {

    @Override
    protected void upgrade() throws Exception {
        Map<String, String[]> scripts = new HashMap<>();
        scripts.put(DatabaseType.MYSQL.name(), mysql);
        scripts.put(DatabaseType.H2.name(), h2);
        scripts.put(DatabaseType.MSSQL.name(), mssql);
        scripts.put(DatabaseType.POSTGRES.name(), postgres);
        runScript(scripts);
    }

    @Override
    protected String getNewSchemaVersion() {
        return "24";
    }

    private final String[] mysql = new String[] {
            "ALTER TABLE templates MODIFY COLUMN xid VARCHAR(100) NOT NULL;",
            "ALTER TABLE userComments MODIFY COLUMN xid VARCHAR(100) NOT NULL;",
            "ALTER TABLE mailingLists MODIFY COLUMN xid VARCHAR(100) NOT NULL;",
            "ALTER TABLE dataSources MODIFY COLUMN xid VARCHAR(100) NOT NULL;",
            "ALTER TABLE dataPoints MODIFY COLUMN xid VARCHAR(100) NOT NULL;",
            "ALTER TABLE eventDetectors MODIFY COLUMN xid VARCHAR(100) NOT NULL;",
            "ALTER TABLE eventHandlers MODIFY COLUMN xid VARCHAR(100) NOT NULL;",
            "ALTER TABLE publishers MODIFY COLUMN xid VARCHAR(100) NOT NULL;",
            "ALTER TABLE jsonData MODIFY COLUMN xid VARCHAR(100) NOT NULL;"
    };
    private final String[] h2 = new String[] {
            "ALTER TABLE templates ALTER COLUMN xid VARCHAR(100) NOT NULL;",
            "ALTER TABLE userComments ALTER COLUMN xid VARCHAR(100) NOT NULL;",
            "ALTER TABLE mailingLists ALTER COLUMN xid VARCHAR(100) NOT NULL;",
            "ALTER TABLE dataSources ALTER COLUMN xid VARCHAR(100) NOT NULL;",
            "ALTER TABLE dataPoints ALTER COLUMN xid VARCHAR(100) NOT NULL;",
            "ALTER TABLE eventDetectors ALTER COLUMN xid VARCHAR(100) NOT NULL;",
            "ALTER TABLE eventHandlers ALTER COLUMN xid VARCHAR(100) NOT NULL;",
            "ALTER TABLE publishers ALTER COLUMN xid VARCHAR(100) NOT NULL;",
            "ALTER TABLE jsonData ALTER COLUMN xid VARCHAR(100) NOT NULL;"
    };
    private final String[] mssql = new String[] {
            "ALTER TABLE templates ALTER COLUMN xid NVARCHAR(100) NOT NULL;",
            "ALTER TABLE userComments ALTER COLUMN xid NVARCHAR(100) NOT NULL;",
            "ALTER TABLE mailingLists ALTER COLUMN xid NVARCHAR(100) NOT NULL;",
            "ALTER TABLE dataSources ALTER COLUMN xid NVARCHAR(100) NOT NULL;",
            "ALTER TABLE dataPoints ALTER COLUMN xid NVARCHAR(100) NOT NULL;",
            "ALTER TABLE eventDetectors ALTER COLUMN xid NVARCHAR(100) NOT NULL;",
            "ALTER TABLE eventHandlers ALTER COLUMN xid NVARCHAR(100) NOT NULL;",
            "ALTER TABLE publishers ALTER COLUMN xid NVARCHAR(100) NOT NULL;",
            "ALTER TABLE jsonData ALTER COLUMN xid NVARCHAR(100) NOT NULL;"
    };
    private final String[] postgres = new String[] {
            "ALTER TABLE templates ALTER COLUMN xid TYPE VARCHAR(100) NOT NULL;",
            "ALTER TABLE userComments ALTER COLUMN xid TYPE VARCHAR(100) NOT NULL;",
            "ALTER TABLE mailingLists ALTER COLUMN xid TYPE VARCHAR(100) NOT NULL;",
            "ALTER TABLE dataSources ALTER COLUMN xid TYPE VARCHAR(100) NOT NULL;",
            "ALTER TABLE dataPoints ALTER COLUMN xid TYPE VARCHAR(100) NOT NULL;",
            "ALTER TABLE eventDetectors ALTER COLUMN xid TYPE VARCHAR(100) NOT NULL;",
            "ALTER TABLE eventHandlers ALTER COLUMN xid TYPE VARCHAR(100) NOT NULL;",
            "ALTER TABLE publishers ALTER COLUMN xid TYPE VARCHAR(100) NOT NULL;",
            "ALTER TABLE jsonData ALTER COLUMN xid TYPE VARCHAR(100) NOT NULL;"
    };
}
