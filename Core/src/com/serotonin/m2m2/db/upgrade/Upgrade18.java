/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db.upgrade;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;

import com.serotonin.m2m2.db.DatabaseType;

public class Upgrade18 extends DBUpgrade {

	@Override
	protected void upgrade() throws Exception {
		//Add the data type column into the database.
		Map<String, String[]> scripts = new HashMap<>();
        scripts.put(DatabaseType.DERBY.name(), H2_MYSQL_CREATE_TABLE);
        scripts.put(DatabaseType.MYSQL.name(), H2_MYSQL_CREATE_TABLE);
        scripts.put(DatabaseType.MSSQL.name(), H2_MYSQL_CREATE_TABLE);
        scripts.put(DatabaseType.H2.name(), H2_MYSQL_CREATE_TABLE);
        runScript(scripts);
        ejt.query(DEPRECATED_EVENT_HANDLER_SELECT, new UpgradeMultipleEventHandlersResultSetExtractor());
        
        scripts.clear();
        scripts.put(DatabaseType.DERBY.name(), H2_MYSQL_DROP_COLUMNS);
        scripts.put(DatabaseType.MYSQL.name(), H2_MYSQL_DROP_COLUMNS);
        scripts.put(DatabaseType.MSSQL.name(), H2_MYSQL_DROP_COLUMNS);
        scripts.put(DatabaseType.H2.name(), H2_MYSQL_DROP_COLUMNS);
        runScript(scripts);
	}

	@Override
	protected String getNewSchemaVersion() {
		return "19";
	}
	
	private static final String[] H2_MYSQL_CREATE_TABLE = { 
	        "CREATE TABLE eventHandlersMapping (eventHandlerId int not null, eventTypeName varchar(32) NOT NULL, eventSubtypeName varchar(32) NOT NULL DEFAULT '', eventTypeRef1 int NOT NULL, eventTypeRef2 int NOT NULL);",
	        "ALTER TABLE eventHandlersMapping ADD CONSTRAINT eventHandlersFk1 FOREIGN KEY (eventHandlerId) REFERENCES eventHandlers(id) ON DELETE CASCADE;",
	        "ALTER TABLE eventHandlersMapping ADD CONSTRAINT handlerMappingUniqueness UNIQUE(eventHandlerId, eventTypeName, eventSubtypeName, eventTypeRef1, eventTypeRef2);"
	};
	
	private static final String[] H2_MYSQL_DROP_COLUMNS = {
	        "ALTER TABLE eventHandlers DROP COLUMN eventTypeName;",
	        "ALTER TABLE eventHandlers DROP COLUMN eventSubtypeName;",
	        "ALTER TABLE eventHandlers DROP COLUMN eventTypeRef1;",
	        "ALTER TABLE eventHandlers DROP COLUMN eventTypeRef2;"
	};
	
	/*
	 * 1. Create new table, can have key constraint to begin with.
	 * 2. Convert existing rows
	 * 3. drop columns
	 */
	
	private static final String DEPRECATED_EVENT_HANDLER_SELECT = "SELECT id, eventTypeName, eventSubtypeName, eventTypeRef1, eventTypeRef2 FROM eventHandlers;";
	
	class DeprecatedEventHandlerRow {
	    int id;
	    String eventTypeName;
	    String eventSubtypeName;
	    int eventTypeRef1;
	    int eventTypeRef2;
	}
	
	class DeprecatedEventHandlerRowMapper implements RowMapper<DeprecatedEventHandlerRow> {

        @Override
        public DeprecatedEventHandlerRow mapRow(ResultSet rs, int rowNum) throws SQLException {
            int i = 0;
            DeprecatedEventHandlerRow dep = new DeprecatedEventHandlerRow();
            dep.id = rs.getInt(++i);
            dep.eventTypeName = rs.getString(++i);
            dep.eventSubtypeName = rs.getString(++i);
            dep.eventTypeRef1 = rs.getInt(++i);
            dep.eventTypeRef2 = rs.getInt(++i);
            return dep;
        }
	    
	}
	
	class UpgradeMultipleEventHandlersResultSetExtractor implements ResultSetExtractor<Void> {
	    @Override
	    public Void extractData(ResultSet rs) throws SQLException, DataAccessException {
	        DeprecatedEventHandlerRowMapper mapper = new DeprecatedEventHandlerRowMapper();
	        while(rs.next()) {
	            DeprecatedEventHandlerRow dep = mapper.mapRow(rs, rs.getRow());
	            addEventHandlerMapping(dep.id, dep.eventTypeName, dep.eventSubtypeName, dep.eventTypeRef1, dep.eventTypeRef2);
	        }
	        return null;
	    }
    	    
        public void addEventHandlerMapping(int eventHandlerId, String typeName, String subtypeName,
                int typeRef1, int typeRef2) {
            if (subtypeName == null)
                ejt.doInsert(
                        "INSERT INTO eventHandlersMapping (eventHandlerId, eventTypeName, eventTypeRef1, eventTypeRef2) values (?,?,?,?)",
                        new Object[] {eventHandlerId, typeName, typeRef1, typeRef2},
                        new int[] {Types.INTEGER, Types.VARCHAR, Types.INTEGER, Types.INTEGER});
            else
                ejt.doInsert(
                        "INSERT INTO eventHandlersMapping (eventHandlerId, eventTypeName, eventSubtypeName, eventTypeRef1, eventTypeRef2) values (?,?,?,?,?)",
                        new Object[] {eventHandlerId, typeName, subtypeName, typeRef1, typeRef2},
                        new int[] {Types.INTEGER, Types.VARCHAR, Types.VARCHAR, Types.INTEGER,
                                Types.INTEGER});
        }
	}
}
