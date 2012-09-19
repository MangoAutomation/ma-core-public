package com.serotonin.m2m2.db.upgrade;

import java.util.HashMap;
import java.util.Map;

import com.serotonin.m2m2.db.DatabaseProxy;

public class Upgrade4 extends DBUpgrade {
    @Override
    public void upgrade() throws Exception {
        // Run the script.
        Map<String, String[]> scripts = new HashMap<String, String[]>();
        scripts.put(DatabaseProxy.DatabaseType.DERBY.name(), derbyScript);
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), mysqlScript);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), mssqlScript);
        runScript(scripts);
    }

    @Override
    protected String getNewSchemaVersion() {
        return "5";
    }

    private final String[] derbyScript = { //
    "ALTER TABLE users ADD COLUMN timezone varchar(50);", //
            "ALTER TABLE dataPoints ADD COLUMN purgeOverride char(1);", //
            "UPDATE dataPoints SET purgeOverride='Y';", //
    };

    private final String[] mssqlScript = { //
    "ALTER TABLE users ADD COLUMN timezone nvarchar(50);", //
            "ALTER TABLE dataPoints ADD COLUMN purgeOverride char(1);", //
            "UPDATE dataPoints SET purgeOverride='Y';", //
    };

    private final String[] mysqlScript = { //
    "ALTER TABLE users ADD COLUMN timezone varchar(50);", //
            "ALTER TABLE dataPoints ADD COLUMN purgeOverride char(1);", //
            "UPDATE dataPoints SET purgeOverride='Y';", //
    };
}
