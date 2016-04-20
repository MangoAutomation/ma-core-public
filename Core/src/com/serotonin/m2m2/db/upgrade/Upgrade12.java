/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Jared Wiltshire
 */
package com.serotonin.m2m2.db.upgrade;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.RowMapper;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.DatabaseProxy;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.definitions.EmailEventHandlerDefinition;
import com.serotonin.m2m2.module.definitions.ProcessEventHandlerDefinition;
import com.serotonin.m2m2.module.definitions.SetPointEventHandlerDefinition;
import com.serotonin.m2m2.vo.event.AbstractEventHandlerVO;
import com.serotonin.m2m2.vo.event.EmailEventHandlerVO;
import com.serotonin.m2m2.vo.event.EventHandlerVO;
import com.serotonin.m2m2.vo.event.ProcessEventHandlerVO;
import com.serotonin.m2m2.vo.event.SetPointEventHandlerVO;
import com.serotonin.util.SerializationHelper;

/**
 * Increases length of users' password field and prepends the hash algorithm name to the hash
 * @author Jared Wiltshire
 *
 */
public class Upgrade12 extends DBUpgrade {

    @Override
    public void upgrade() throws Exception {
        // get hash algorithm using the old default
        String hashAlgorithm = Common.envProps.getString("security.hashAlgorithm", "SHA-1");
        
        // Run the script.
        Map<String, String[]> scripts = new HashMap<>();
        
        scripts.put(DatabaseProxy.DatabaseType.DERBY.name(), new String[] {
            "ALTER TABLE users ALTER COLUMN password SET DATA TYPE VARCHAR(255);",
            "UPDATE users SET password  = '{" + hashAlgorithm + "}' || password;",
            "ALTER TABLE eventHandlers ADD COLUMN eventHandlerType VARCHAR(40);"
        });
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), new String[] {
            "ALTER TABLE users MODIFY password VARCHAR(255) NOT NULL;",
            "UPDATE users SET password  = CONCAT('{" + hashAlgorithm + "}', password);",
            "ALTER TABLE eventHandlers ADD COLUMN eventHandlerType VARCHAR(40);"
        });
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), new String[] {
            "ALTER TABLE users ALTER COLUMN password nvarchar(255) NOT NULL;",
            "UPDATE users SET password  = CONCAT('{" + hashAlgorithm + "}', password);",
            "ALTER TABLE eventHandlers ADD COLUMN eventHandlerType nvarchar(40);"
        });
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), new String[] {
            "ALTER TABLE users ALTER COLUMN password VARCHAR(255) NOT NULL;",
            "UPDATE users SET password  = CONCAT('{" + hashAlgorithm + "}', password);",
            "ALTER TABLE eventHandlers ADD COLUMN eventHandlerType VARCHAR(40);"
        });
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
    
    
    private static final String EVENT_HANDLER_SELECT = "select id, xid, alias, data from eventHandlers ";

	@SuppressWarnings("deprecation")
    private void upgradeEventHandlers(){

		List<EventHandlerVO> handlers = this.ejt.query(EVENT_HANDLER_SELECT, new EventHandlerRowMapper());
    	
    	//Convert them and update the database with the new handlers
    	for(EventHandlerVO vo : handlers){
    		switch(vo.getHandlerType()){
    			case EventHandlerVO.TYPE_EMAIL:
    				EmailEventHandlerVO emailHandler = new EmailEventHandlerVO();
    				emailHandler.setDefinition(ModuleRegistry.getEventHandlerDefinition(EmailEventHandlerDefinition.TYPE_NAME));
    				emailHandler.setDefinition(ModuleRegistry.getEventHandlerDefinition(EmailEventHandlerDefinition.TYPE_NAME));
    				emailHandler.setActiveRecipients(vo.getActiveRecipients());
    				emailHandler.setSendEscalation(vo.isSendEscalation());
    				emailHandler.setEscalationDelayType(vo.getEscalationDelayType());
    				emailHandler.setEscalationDelay(vo.getEscalationDelay());
    		        emailHandler.setEscalationRecipients(vo.getEscalationRecipients());
    		        emailHandler.setSendInactive(vo.isSendInactive());
    		        emailHandler.setInactiveOverride(vo.isInactiveOverride());
    		        emailHandler.setInactiveRecipients(vo.getInactiveRecipients());
    		        emailHandler.setIncludeSystemInfo(vo.isIncludeSystemInfo());
    		        emailHandler.setIncludePointValueCount(vo.getIncludePointValueCount());
    		        emailHandler.setIncludeLogfile(vo.isIncludeLogfile());
    		        upgradeEventHandler(emailHandler);
    		    break;
    			case EventHandlerVO.TYPE_PROCESS:
    		        ProcessEventHandlerVO processHandler = new ProcessEventHandlerVO();
    		        processHandler.setDefinition(ModuleRegistry.getEventHandlerDefinition(ProcessEventHandlerDefinition.TYPE_NAME));
    		        processHandler.setActiveProcessCommand(vo.getActiveProcessCommand());
    		        processHandler.setActiveProcessTimeout(vo.getActiveProcessTimeout());
    		        processHandler.setInactiveProcessCommand(vo.getInactiveProcessCommand());
    		        processHandler.setInactiveProcessTimeout(vo.getInactiveProcessTimeout());
    		        upgradeEventHandler(processHandler);
    			break;
    			case EventHandlerVO.TYPE_SET_POINT:
    		        SetPointEventHandlerVO setPointHandler = new SetPointEventHandlerVO();
    		        setPointHandler.setDefinition(ModuleRegistry.getEventHandlerDefinition(SetPointEventHandlerDefinition.TYPE_NAME));
    		        setPointHandler.setTargetPointId(vo.getTargetPointId());
    		        setPointHandler.setActiveAction(vo.getActiveAction());
    		        setPointHandler.setActiveValueToSet(vo.getActiveValueToSet());
    		        setPointHandler.setActivePointId(vo.getActivePointId());
    		        setPointHandler.setInactiveAction(vo.getInactiveAction());
    		        setPointHandler.setInactiveValueToSet(vo.getInactiveValueToSet());
    		        setPointHandler.setInactivePointId(vo.getInactivePointId());
    		        upgradeEventHandler(setPointHandler);
    			break;
    			default:
    				throw new ShouldNeverHappenException("Unknown event detector type: " + vo.getHandlerType());
    		}
    	}
    	
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
