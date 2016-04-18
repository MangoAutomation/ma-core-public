/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Jared Wiltshire
 */
package com.serotonin.m2m2.db.upgrade;

import java.util.HashMap;
import java.util.Map;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.DatabaseProxy;

/**
 * Increases length of users' password field and prepends the hash algorithm name to the hash
 * @author Jared Wiltshire
 *
 */
public class Upgrade12 extends DBUpgrade {

    @Override
    public void upgrade() throws Exception {
        // get hash algorithm using the old default
        String hashAlgorithm = Common.envProps.getString("security.hashAlgorithm", "SHA-1");
        
        // Run the script.
        Map<String, String[]> scripts = new HashMap<>();
        
        scripts.put(DatabaseProxy.DatabaseType.DERBY.name(), new String[] {
            "ALTER TABLE users ALTER COLUMN password SET DATA TYPE VARCHAR(255);",
            "UPDATE users SET password  = '{" + hashAlgorithm + "}' || password;",
        });
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), new String[] {
            "ALTER TABLE users MODIFY password VARCHAR(255) NOT NULL;",
            "UPDATE users SET password  = CONCAT('{" + hashAlgorithm + "}', password);",
        });
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), new String[] {
            "ALTER TABLE users ALTER COLUMN password nvarchar(255) NOT NULL;",
            "UPDATE users SET password  = CONCAT('{" + hashAlgorithm + "}', password);",
        });
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), new String[] {
            "ALTER TABLE users ALTER COLUMN password VARCHAR(255) NOT NULL;",
            "UPDATE users SET password  = CONCAT('{" + hashAlgorithm + "}', password);",
        });
        runScript(scripts);

    }

    @Override
    protected String getNewSchemaVersion() {
        return "13";
    }
}
