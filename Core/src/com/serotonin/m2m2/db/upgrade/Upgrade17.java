/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.db.upgrade;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.jdbc.core.RowCallbackHandler;

import com.serotonin.m2m2.db.DatabaseType;

/**
 * @author Jared Wiltshire
 */
public class Upgrade17 extends DBUpgrade {
	@Override
	protected void upgrade() throws Exception {
        Map<String, String[]> scripts = new HashMap<>();
        scripts.put(DatabaseType.DERBY.name(), derby);
        scripts.put(DatabaseType.MYSQL.name(), mysql);
        scripts.put(DatabaseType.MSSQL.name(), mssql);
        scripts.put(DatabaseType.H2.name(), h2);
        
        //Check event detector lengths to ensure we won't make them longer than 50 (the max XID column length)
        this.ejt.query("SELECT id,xid FROM eventDetectors", new RowCallbackHandler() {

            @Override
            public void processRow(ResultSet rs) throws SQLException {
                int id = rs.getInt(1);
                String xid = rs.getString(2);
                String newXid = xid + "_" + id;
                if(newXid.length() > 50) {
                    //Trim and update
                    int over = newXid.length() - 50;
                    xid = xid.substring(over, xid.length());
                    ejt.update("UPDATE eventDetectors SET xid=? WHERE id=?", new Object[] {xid, id});
                }
            }
            
        });
        
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
