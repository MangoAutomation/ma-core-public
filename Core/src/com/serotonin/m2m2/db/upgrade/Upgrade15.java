package com.serotonin.m2m2.db.upgrade;

import java.util.HashMap;
import java.util.Map;

import com.serotonin.m2m2.db.DatabaseProxy;

public class Upgrade15 extends DBUpgrade {

	@Override
	protected void upgrade() throws Exception {
		// Run the script.
        Map<String, String[]> scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.DERBY.name(), upgrade);
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), upgrade);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), upgrade);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), upgrade);
        runScript(scripts);
	}

	@Override
	protected String getNewSchemaVersion() {
		return "16";
	}

	private static final String[] upgrade = {
		"ALTER TABLE dataPoints ADD COLUMN rollup INT;",
	    "UPDATE dataPoints SET rollup=0;"
	};
}
