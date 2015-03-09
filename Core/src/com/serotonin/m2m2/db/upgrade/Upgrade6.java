package com.serotonin.m2m2.db.upgrade;

import java.util.HashMap;
import java.util.Map;

import com.serotonin.m2m2.db.DatabaseProxy;

public class Upgrade6 extends DBUpgrade {
    @Override
    public void upgrade() throws Exception {
        // Run the script.
        Map<String, String[]> scripts = new HashMap<String, String[]>();
        scripts.put(DatabaseProxy.DatabaseType.DERBY.name(), derbyScript);
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), mysqlScript);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), mssqlScript);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), new String[0]);
        runScript(scripts);

        ejt.update("UPDATE users SET muted=?", new Object[] { boolToChar(false) });
    }

    @Override
    protected String getNewSchemaVersion() {
        return "7";
    }

    private final String[] derbyScript = { //
    "ALTER TABLE users ADD COLUMN muted char(1);", //
    };

    private final String[] mssqlScript = { //
    "ALTER TABLE users ADD COLUMN muted char(1);", //
    };

    private final String[] mysqlScript = { //
    "ALTER TABLE users ADD COLUMN muted char(1);", //
    };
}
