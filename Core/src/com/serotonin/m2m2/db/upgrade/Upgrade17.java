/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.db.upgrade;

import java.util.HashMap;
import java.util.Map;

import com.serotonin.m2m2.db.DatabaseProxy;

/**
 * @author Jared Wiltshire
 */
public class Upgrade17 extends DBUpgrade {
	@Override
	protected void upgrade() throws Exception {
        Map<String, String[]> scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.DERBY.name(), derby);
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), mysql);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), mssql);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), h2);
        runScript(scripts);
	}

	@Override
	protected String getNewSchemaVersion() {
		return "18";
	}
	
	private final String[] derby = new String[] {
        "ALTER TABLE eventDetectors DROP CONSTRAINT eventDetectorsUn1;",
        "UPDATE eventDetectors SET xid = xid || '_' || id;",
        "ALTER TABLE eventDetectors ADD CONSTRAINT eventDetectorsUn1 UNIQUE (xid);"
    };
	
    private final String[] mysql = new String[] {
        "ALTER TABLE eventDetectors DROP INDEX eventDetectorsUn1;",
        "UPDATE eventDetectors SET xid = CONCAT(xid, '_', id);",
        "ALTER TABLE eventDetectors ADD CONSTRAINT eventDetectorsUn1 UNIQUE (xid);"
    };
    
    private final String[] mssql = new String[] {
        "ALTER TABLE eventDetectors DROP CONSTRAINT eventDetectorsUn1;",
        "UPDATE eventDetectors SET xid = xid + '_' + id;",
        "ALTER TABLE eventDetectors ADD CONSTRAINT eventDetectorsUn1 UNIQUE (xid);"
    };
    
    private final String[] h2 = new String[] {
        "ALTER TABLE eventDetectors DROP CONSTRAINT eventDetectorsUn1;",
        "UPDATE eventDetectors SET xid = CONCAT(xid, '_', id);",
        "ALTER TABLE eventDetectors ADD CONSTRAINT eventDetectorsUn1 UNIQUE (xid);"
    };
}
