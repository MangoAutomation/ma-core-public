package com.serotonin.m2m2.db.upgrade;

import java.util.HashMap;
import java.util.Map;

import com.serotonin.m2m2.db.DatabaseProxy;

public class Upgrade7 extends DBUpgrade {
    @Override
    public void upgrade() throws Exception {
        // Run the script.
        Map<String, String[]> scripts = new HashMap<String, String[]>();
        scripts.put(DatabaseProxy.DatabaseType.DERBY.name(), derbyScript);
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), mysqlScript);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), mssqlScript);
        runScript(scripts);

        ejt.update("UPDATE users SET muted=?", new Object[] { boolToChar(false) });
    }

    @Override
    protected String getNewSchemaVersion() {
        return "8";
    }

    private final String[] derbyScript = { };

    private final String[] mssqlScript = { };

    private final String[] mysqlScript = { //
    	"ALTER TABLE pointValues ENGINE=InnoDB;",//
    	"ALTER TABLE pointValueAnnotations ENGINE=InnoDB;",
    };
}
