package com.serotonin.m2m2.db.upgrade;

import java.util.HashMap;
import java.util.Map;

import com.serotonin.m2m2.db.DatabaseProxy;

public class Upgrade16 extends DBUpgrade {
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
		return "17";
	}

	private static final String[] upgrade = {
			"update systemSettings set settingvalue='4' where settingname like 'systemEventalarmLevel%' and settingvalue='2';",
			"update systemSettings set settingvalue='5' where settingname like 'systemEventalarmLevel%' and settingvalue='3';",
			"update systemSettings set settingvalue='6' where settingname like 'systemEventalarmLevel%' and settingvalue='4';",
			"update events set alarmLevel=alarmLevel+2 where alarmLevel>=2;",
			"update users set receiveAlarmEmails=receiveAlarmEmails+2 where receiveAlarmEmails>=2;",
			"update audit set alarmLevel=alarmLevel+2 where alarmLevel>=2;"
	};
}
