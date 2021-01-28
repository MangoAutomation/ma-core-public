package com.serotonin.m2m2.db.upgrade;

import java.util.HashMap;
import java.util.Map;

import com.serotonin.m2m2.db.DatabaseType;

public class Upgrade7 extends DBUpgrade {
    @Override
    public void upgrade() throws Exception {
        // Run the script.
        Map<String, String[]> scripts = new HashMap<String, String[]>();
        scripts.put(DatabaseType.DERBY.name(), derbyScript);
        scripts.put(DatabaseType.MYSQL.name(), mysqlScript);
        scripts.put(DatabaseType.MSSQL.name(), mssqlScript);
        scripts.put(DatabaseType.H2.name(), new String[0]);
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
    	//Removed September 14, 2014 as this is too slow and is causing difficulty in upgrades 
         //"ALTER TABLE pointValues ENGINE=InnoDB;",//
    	//"ALTER TABLE pointValueAnnotations ENGINE=InnoDB;",
    };
}
