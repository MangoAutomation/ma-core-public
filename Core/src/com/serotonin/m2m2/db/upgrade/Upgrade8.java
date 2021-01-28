package com.serotonin.m2m2.db.upgrade;

import java.util.HashMap;
import java.util.Map;

import com.serotonin.m2m2.db.DatabaseType;

public class Upgrade8 extends DBUpgrade {
    @Override
    public void upgrade() throws Exception {
        // Run the script.
        Map<String, String[]> scripts = new HashMap<String, String[]>();
        scripts.put(DatabaseType.DERBY.name(), derbyScript);
        scripts.put(DatabaseType.MYSQL.name(), mysqlScript);
        scripts.put(DatabaseType.MSSQL.name(), mssqlScript);
        scripts.put(DatabaseType.H2.name(), new String[0]);
        runScript(scripts);
    }

    @Override
    protected String getNewSchemaVersion() {
        return "9";
    }

    private final String[] derbyScript = {
    		"create index performance1 on userEvents (userId ASC, silenced ASC);",
    		"create index performance1 on events (activeTs ASC);"
    };

    private final String[] mssqlScript = { };

    private final String[] mysqlScript = {
    		"ALTER TABLE `userEvents` ADD INDEX `performance1` (`userId` ASC, `silenced` ASC);",
    		"ALTER TABLE `events` ADD INDEX `performance1` (`activeTs` ASC);"
    };
}
