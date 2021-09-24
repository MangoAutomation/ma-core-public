package com.serotonin.m2m2.db.upgrade;

import java.util.HashMap;
import java.util.Map;

import com.serotonin.m2m2.db.DatabaseType;
import com.serotonin.m2m2.db.dao.BaseDao;

public class Upgrade6 extends DBUpgrade {
    @Override
    public void upgrade() throws Exception {
        // Run the script.
        Map<String, String[]> scripts = new HashMap<String, String[]>();
        scripts.put(DatabaseType.DERBY.name(), derbyScript);
        scripts.put(DatabaseType.MYSQL.name(), mysqlScript);
        scripts.put(DatabaseType.MSSQL.name(), mssqlScript);
        scripts.put(DatabaseType.H2.name(), mysqlScript);
        runScript(scripts);

        ejt.update("UPDATE users SET muted=?", BaseDao.boolToChar(false));
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
