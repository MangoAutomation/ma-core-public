/**
 * @copyright 2018 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Phillip Dunlap
 */
package com.serotonin.m2m2.db.upgrade;

import java.util.HashMap;
import java.util.Map;

import com.serotonin.m2m2.db.DatabaseProxy;

/**
 * Convert h2 longblob colums to binary
 *
 * @author Terry Packer
 */
public class Upgrade25 extends DBUpgrade {

    @Override
    protected void upgrade() throws Exception {
        Map<String, String[]> scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), sql);
        runScript(scripts);
    }

    @Override
    protected String getNewSchemaVersion() {
        return "26";
    }
    
    private String[] sql = new String[]{
            "ALTER TABLE templates ALTER COLUMN data BINARY;",
            "ALTER TABLE dataSources ALTER COLUMN data BINARY;",
            "ALTER TABLE dataSources ALTER COLUMN rtdata BINARY;",
            "ALTER TABLE dataPoints ALTER COLUMN data BINARY;",
            "ALTER TABLE eventHandlers ALTER COLUMN data BINARY;",
            "ALTER TABLE publishers ALTER COLUMN data BINARY;",
            "ALTER TABLE publishers ALTER COLUMN rtdata BINARY;",
    };
    
}
