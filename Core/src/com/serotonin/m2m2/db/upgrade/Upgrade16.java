package com.serotonin.m2m2.db.upgrade;

import java.util.HashMap;
import java.util.Map;

import com.serotonin.m2m2.db.DatabaseType;

public class Upgrade16 extends DBUpgrade {
	@Override
	protected void upgrade() throws Exception {
		//Add receiveAlarmEmails column to mailing lists
		Map<String, String[]> scripts = new HashMap<>();
        scripts.put(DatabaseType.DERBY.name(), derbyColumn);
        scripts.put(DatabaseType.MYSQL.name(), mysqlColumn);
        scripts.put(DatabaseType.MSSQL.name(), mssqlColumn);
        scripts.put(DatabaseType.H2.name(), h2Column);
        runScript(scripts);
		
		// Modify the old event levels for things
        scripts.clear();
        scripts.put(DatabaseType.DERBY.name(), upgrade);
        scripts.put(DatabaseType.MYSQL.name(), upgrade);
        scripts.put(DatabaseType.MSSQL.name(), upgrade);
        scripts.put(DatabaseType.H2.name(), upgrade);
        runScript(scripts);
	}

	@Override
	protected String getNewSchemaVersion() {
		return "17";
	}
	
	private static final String[] h2Column = {
			"ALTER TABLE mailingLists ADD COLUMN receiveAlarmEmails INT;",
			"UPDATE mailingLists SET receiveAlarmEmails=-3;",
			"ALTER TABLE mailingLists ALTER COLUMN receiveAlarmEmails INT NOT NULL;"
	};
	private static final String[] mysqlColumn = {
			"ALTER TABLE mailingLists ADD COLUMN receiveAlarmEmails INT;",
			"UPDATE mailingLists SET receiveAlarmEmails=-3;",
			"ALTER TABLE mailingLists MODIFY COLUMN receiveAlarmEmails INT NOT NULL;"
	};
	private static final String[] derbyColumn = {
			"ALTER TABLE mailingLists ADD COLUMN receiveAlarmEmails INT;",
			"UPDATE mailingLists SET receiveAlarmEmails=-3;",
			"ALTER TABLE mailingLists ALTER COLUMN receiveAlarmEmails INT NOT NULL;"
	};
	private static final String[] mssqlColumn = {
			"ALTER TABLE mailingLists ADD COLUMN receiveAlarmEmails INT;",
			"UPDATE mailingLists SET receiveAlarmEmails=-3;",
			"ALTER TABLE mailingLists ALTER COLUMN receiveAlarmEmails INT NOT NULL;"
	};

	private static final String[] upgrade = {
			"UPDATE systemSettings SET settingvalue='6' WHERE settingname LIKE 'systemEventAlarmLevel%' AND settingvalue='4';",
			"UPDATE systemSettings SET settingvalue='5' WHERE settingname LIKE 'systemEventAlarmLevel%' AND settingvalue='3';",
			"UPDATE systemSettings SET settingvalue='4' WHERE settingname LIKE 'systemEventAlarmLevel%' AND settingvalue='2';",
			"UPDATE events SET alarmLevel=alarmLevel+2 WHERE alarmLevel>=2 AND alarmLevel<=4;",
			"UPDATE users SET receiveAlarmEmails=receiveAlarmEmails+2 WHERE receiveAlarmEmails>=2 AND receiveAlarmEmails<=4;",
			"UPDATE audit SET alarmLevel=alarmLevel+2 WHERE alarmLevel>=2 AND alarmLevel<=4;"
	};
}
