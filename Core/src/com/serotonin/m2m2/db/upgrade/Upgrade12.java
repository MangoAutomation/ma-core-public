/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.db.upgrade;

import java.util.HashMap;
import java.util.Map;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.DatabaseProxy;
/**
 * Upgrade to add template system
 * 
 * @author Terry Packer
 *
 */
public class Upgrade12 extends DBUpgrade {


    @Override
    public void upgrade() throws Exception {
    	// get hash algorithm using the old default
    	String hashAlgorithm = Common.envProps.getString("security.hashAlgorithm", "SHA-1");

    	String[] mysqlScript = {
	        "ALTER TABLE users MODIFY password VARCHAR(255) NOT NULL;",
	        "UPDATE users SET password  = CONCAT('{" + hashAlgorithm + "}', password);",
	        
	        "ALTER TABLE eventHandlers ADD COLUMN eventHandlerType VARCHAR(40);"
	   
	    	"ALTER TABLE dataPoints ADD INDEX nameIndex (name ASC);",
	    	"ALTER TABLE dataPoints ADD INDEX deviceNameIndex (deviceName ASC);",
	    	"ALTER TABLE dataPoints ADD INDEX pointFolderIndex (pointFolderId ASC);",
	    	"ALTER TABLE dataPoints ADD INDEX dataSourceIndex (dataSourceId ASC);",
	    	"ALTER TABLE jsonData ADD COLUMN publicData char(1);",
	        "UPDATE jsonData SET publicData='N';",
	
	    };
	    String[] derbyScript = {
	        "ALTER TABLE users ALTER COLUMN password SET DATA TYPE VARCHAR(255);",
	        "UPDATE users SET password  = '{" + hashAlgorithm + "}' || password;",
	    
	    	"ALTER TABLE eventHandlers ADD COLUMN eventHandlerType VARCHAR(40);

	   		"CREATE INDEX nameIndex on dataPoints (name ASC);",
	   		"CREATE INDEX deviceNameIndex on dataPoints (deviceName ASC);",
	   		"CREATE INDEX pointFolderIndex on dataPoints (pointFolderId ASC);",
	   		"CREATE INDEX dataSourceIndex on dataPoints (dataSourceId ASC);",
	    	"ALTER TABLE jsonData ADD COLUMN publicData char(1);",
	        "UPDATE jsonData SET publicData='N';",
	    };    
	
	    String[] h2Script = {
	        "ALTER TABLE users ALTER COLUMN password VARCHAR(255) NOT NULL;",
	        "UPDATE users SET password  = CONCAT('{" + hashAlgorithm + "}', password);",
	        
	        "ALTER TABLE eventHandlers ADD COLUMN eventHandlerType VARCHAR(40);"
	
	    	"CREATE INDEX nameIndex on dataPoints (`name` ASC);",
	    	"CREATE INDEX deviceNameIndex on dataPoints (`deviceName` ASC);",
	    	"CREATE INDEX pointFolderIndex on dataPoints (`pointFolderId` ASC);",
	    	"CREATE INDEX dataSourceIndex on dataPoints (`dataSourceId` ASC);", 
	    	"ALTER TABLE jsonData ADD COLUMN publicData char(1);",
	        "UPDATE jsonData SET publicData='N';",
	    };
	    
	   	String[] mssqlScript = {
	        "ALTER TABLE users ALTER COLUMN password nvarchar(255) NOT NULL;",
	        "UPDATE users SET password  = CONCAT('{" + hashAlgorithm + "}', password);",
	        
	        "ALTER TABLE eventHandlers ADD COLUMN eventHandlerType nvarchar(40);"
	        
	   		"CREATE INDEX nameIndex on dataPoints (name ASC);",
	   		"CREATE INDEX deviceNameIndex on dataPoints (deviceName ASC);",
	   		"CREATE INDEX pointFolderIndex on dataPoints (pointFolderId ASC);",
	   		"CREATE INDEX dataSourceIndex on dataPoints (dataSourceId ASC);",
	    	"ALTER TABLE jsonData ADD COLUMN publicData char(1);",
	        "UPDATE jsonData SET publicData='N';",    
	    };
    
        // Run the script.
        Map<String, String[]> scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.DERBY.name(), derbyScript);
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), mysqlScript);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), mssqlScript);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), h2Script);
        scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), mysqlScript);
        runScript(scripts);
        
        upgradeEventHandlers();
        
        //Now make column not null
        scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.DERBY.name(), new String[] {
            "ALTER TABLE eventHandlers ALTER COLUMN eventHandlerType NOT NULL;",
        });
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), new String[] {
            "ALTER TABLE eventHandlers MODIFY COLUMN eventHandlerType VARCHAR(40) NOT NULL;",
        });
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), new String[] {
            "ALTER TABLE eventHandlers ALTER COLUMN eventHandlerType nvarchar(40) NOT NULL;",
        });
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), new String[] {
            "ALTER TABLE eventHandlers MODIFY COLUMN eventHandlerType VARCHAR(40) NOT NULL;",
        });
        runScript(scripts);

    }

	/**
	 * Upgrade a handler in the DB
	 * @param handler
	 */
    void upgradeEventHandler(AbstractEventHandlerVO handler) {
        ejt.update("update eventHandlers set xid=?, alias=?, eventHandlerType=?, data=? where id=?", new Object[] { handler.getXid(),
                handler.getAlias(), handler.getDefinition().getEventHandlerTypeName(), SerializationHelper.writeObject(handler), handler.getId() }, new int[] {
                Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.BINARY, Types.INTEGER });
    }
	
    @SuppressWarnings("deprecation")
	class EventHandlerRowMapper implements RowMapper<EventHandlerVO> {
		@Override
        public EventHandlerVO mapRow(ResultSet rs, int rowNum) throws SQLException {
            EventHandlerVO h = (EventHandlerVO) SerializationHelper.readObjectInContext(rs.getBinaryStream(4));
            h.setId(rs.getInt(1));
            h.setXid(rs.getString(2));
            h.setAlias(rs.getString(3));
            return h;
        }
    }

    @Override
    protected String getNewSchemaVersion() {
        return "13";
    }
}
