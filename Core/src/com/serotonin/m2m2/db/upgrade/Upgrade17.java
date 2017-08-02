package com.serotonin.m2m2.db.upgrade;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.serotonin.m2m2.db.DatabaseProxy;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.vo.DataPointVO;

public class Upgrade17 extends DBUpgrade {

	@Override
	protected void upgrade() throws Exception {
		//Add the data type column into the database.
		Map<String, String[]> scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.DERBY.name(), addColumn);
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), addColumn);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), addColumn);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), addColumn);
        runScript(scripts);
        
        //not using data type id to deserialize, so we don't need a legacy row mapper here
        List<DataPointVO> allPoints = DataPointDao.instance.getAll(); 
        for(DataPointVO dpvo : allPoints)
        	DataPointDao.instance.save(dpvo); //save all of them so that the data type ID gets written back out
        
        scripts.clear();
        scripts.put(DatabaseProxy.DatabaseType.DERBY.name(), alterColumn);
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), modifyColumn);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), alterColumn);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), alterColumn);
        runScript(scripts);
	}

	@Override
	protected String getNewSchemaVersion() {
		return "18";
	}

	private static final String[] addColumn = {
			"ALTER TABLE dataPoints ADD COLUMN dataTypeId INT;",
	};
	private static final String[] alterColumn = {
			"ALTER TABLE dataPoints ALTER COLUMN dataTypeId INT NOT NULL;"
	};
	private static final String[] modifyColumn = {
			"ALTER TABLE dataPoints MODIFY COLUMN dataTypeId INT NOT NULL;"
	};
}
