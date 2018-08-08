package com.serotonin.m2m2.db.upgrade;

import java.util.HashMap;
import java.util.Map;

import com.serotonin.m2m2.db.DatabaseProxy;

public class Upgrade1 extends DBUpgrade {
    @Override
    public void upgrade() throws Exception {
        // Run the script.
        Map<String, String[]> scripts = new HashMap<String, String[]>();
        scripts.put(DatabaseProxy.DatabaseType.DERBY.name(), derbyScript2);
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), mysqlScript2);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), mssqlScript2);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), mysqlScript2);
        runScript(scripts);
    }

    @Override
    protected String getNewSchemaVersion() {
        return "2";
    }

    private final String[] derbyScript2 = { //
    "alter table publishers add column rtdata blob;", //
    };

    private final String[] mssqlScript2 = { //
    "alter table publishers add column rtdata image;", //
    };

    private final String[] mysqlScript2 = { //
    "alter table publishers add column rtdata longblob;", //
    };
}
