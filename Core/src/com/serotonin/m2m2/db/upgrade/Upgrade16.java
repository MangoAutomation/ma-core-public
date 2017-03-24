package com.serotonin.m2m2.db.upgrade;

import java.util.HashMap;
import java.util.Map;

import com.serotonin.m2m2.db.DatabaseProxy;

public class Upgrade16 extends DBUpgrade {
	@Override
	protected void upgrade() throws Exception {
		//Add receiveAlarmEmails column to mailing lists
		Map<String, String[]> scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.DERBY.name(), derbyColumn);
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), mysqlColumn);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), mssqlColumn);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), h2Column);
        runScript(scripts);
		
		// Modify the old event levels for things
        scripts.clear();
        scripts.put(DatabaseProxy.DatabaseType.DERBY.name(), upgrade);
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), upgrade);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), upgrade);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), upgrade);
        runScript(scripts);
	}

	@Override
	protected String getNewSchemaVersion() {
		return "17";
	}
	
	private static final String[] h2Column = {
			"ALTER TABLE mailingLists ADD COLUMN receiveAlarmEmails INT NOT NULL DEFAULT -3;"
	};
	private static final String[] mysqlColumn = {
			"ALTER TABLE mailingLists ADD COLUMN receiveAlarmEmails INT NOT NULL DEFAULT -3;"
	};
	private static final String[] derbyColumn = {
			"ALTER TABLE mailingLists ADD COLUMN receiveAlarmEmails INT NOT NULL DEFAULT -3;"
	};
	private static final String[] mssqlColumn = {
			"ALTER TABLE mailingLists ADD COLUMN receiveAlarmEmails INT NOT NULL DEFAULT -3;"
	};

	private static final String[] upgrade = {
			"UPDATE systemSettings SET settingvalue='6' WHERE settingname LIKE 'systemEventAlarmLevel%' AND settingvalue='4';",
			"UPDATE systemSettings SET settingvalue='5' WHERE settingname LIKE 'systemEventAlarmLevel%' AND settingvalue='3';",
			"UPDATE systemSettings SET settingvalue='4' WHERE settingname LIKE 'systemEventAlarmLevel%' AND settingvalue='2';",
			"UPDATE events SET alarmLevel=alarmLevel+2 WHERE alarmLevel>=2;",
			"UPDATE users SET receiveAlarmEmails=receiveAlarmEmails+2 WHERE receiveAlarmEmails>=2;",
			"UPDATE audit SET alarmLevel=alarmLevel+2 WHERE alarmLevel>=2;"
	};
}
