/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.db.upgrade;

import java.util.HashMap;
import java.util.Map;

import com.serotonin.m2m2.db.DatabaseProxy;
/**
 * Upgrade to add template system
 * 
 * @author Terry Packer
 *
 */
public class Upgrade12 extends DBUpgrade {

    @Override
    public void upgrade() throws Exception {
        // Run the script.
        Map<String, String[]> scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.DERBY.name(), derbyScript);
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), mysqlScript);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), derbyScript);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), h2Script);
        runScript(scripts);

    }

    @Override
    protected String getNewSchemaVersion() {
        return "13";
    }

    private final String[] mysqlScript = {
    	"ALTER TABLE dataPoints ADD INDEX nameIndex (name ASC);",
    	"ALTER TABLE dataPoints ADD INDEX deviceNameIndex (deviceName ASC);"
    };
    private final String[] derbyScript = {
   		"CREATE INDEX nameIndex on dataPoints (name ASC);",
   		"CREATE INDEX deviceNameIndex on dataPoints (deviceName ASC);"
    };    

    private final String[] h2Script = {
    	"CREATE INDEX nameIndex on dataPoints (`name` ASC);",
    	"CREATE INDEX deviceNameIndex on dataPoints (`deviceName` ASC);"	
    };

}
