/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.db.upgrade;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serotonin.json.JsonException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.DatabaseProxy;
import com.serotonin.m2m2.db.dao.EventDetectorDao;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.definitions.event.detectors.AlphanumericRegexStateEventDetectorDefinition;
import com.serotonin.m2m2.module.definitions.event.detectors.AlphanumericStateEventDetectorDefinition;
import com.serotonin.m2m2.module.definitions.event.detectors.AnalogChangeEventDetectorDefinition;
import com.serotonin.m2m2.module.definitions.event.detectors.AnalogHighLimitEventDetectorDefinition;
import com.serotonin.m2m2.module.definitions.event.detectors.AnalogLowLimitEventDetectorDefinition;
import com.serotonin.m2m2.module.definitions.event.detectors.AnalogRangeEventDetectorDefinition;
import com.serotonin.m2m2.module.definitions.event.detectors.BinaryStateEventDetectorDefinition;
import com.serotonin.m2m2.module.definitions.event.detectors.MultistateStateEventDetectorDefinition;
import com.serotonin.m2m2.module.definitions.event.detectors.NegativeCusumEventDetectorDefinition;
import com.serotonin.m2m2.module.definitions.event.detectors.NoChangeEventDetectorDefinition;
import com.serotonin.m2m2.module.definitions.event.detectors.NoUpdateEventDetectorDefinition;
import com.serotonin.m2m2.module.definitions.event.detectors.PointChangeEventDetectorDefinition;
import com.serotonin.m2m2.module.definitions.event.detectors.PositiveCusumEventDetectorDefinition;
import com.serotonin.m2m2.module.definitions.event.detectors.SmoothnessEventDetectorDefinition;
import com.serotonin.m2m2.module.definitions.event.detectors.StateChangeCountEventDetectorDefinition;
import com.serotonin.m2m2.module.definitions.event.handlers.EmailEventHandlerDefinition;
import com.serotonin.m2m2.module.definitions.event.handlers.ProcessEventHandlerDefinition;
import com.serotonin.m2m2.module.definitions.event.handlers.SetPointEventHandlerDefinition;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.vo.event.AbstractEventHandlerVO;
import com.serotonin.m2m2.vo.event.EmailEventHandlerVO;
import com.serotonin.m2m2.vo.event.EventHandlerVO;
import com.serotonin.m2m2.vo.event.PointEventDetectorVO;
import com.serotonin.m2m2.vo.event.ProcessEventHandlerVO;
import com.serotonin.m2m2.vo.event.SetPointEventHandlerVO;
import com.serotonin.m2m2.vo.event.detector.AbstractEventDetectorVO;
import com.serotonin.m2m2.vo.event.detector.AlphanumericRegexStateDetectorVO;
import com.serotonin.m2m2.vo.event.detector.AlphanumericStateDetectorVO;
import com.serotonin.m2m2.vo.event.detector.AnalogChangeDetectorVO;
import com.serotonin.m2m2.vo.event.detector.AnalogHighLimitDetectorVO;
import com.serotonin.m2m2.vo.event.detector.AnalogLowLimitDetectorVO;
import com.serotonin.m2m2.vo.event.detector.AnalogRangeDetectorVO;
import com.serotonin.m2m2.vo.event.detector.BinaryStateDetectorVO;
import com.serotonin.m2m2.vo.event.detector.MultistateStateDetectorVO;
import com.serotonin.m2m2.vo.event.detector.NegativeCusumDetectorVO;
import com.serotonin.m2m2.vo.event.detector.NoChangeDetectorVO;
import com.serotonin.m2m2.vo.event.detector.NoUpdateDetectorVO;
import com.serotonin.m2m2.vo.event.detector.PointChangeDetectorVO;
import com.serotonin.m2m2.vo.event.detector.PositiveCusumDetectorVO;
import com.serotonin.m2m2.vo.event.detector.SmoothnessDetectorVO;
import com.serotonin.m2m2.vo.event.detector.StateChangeCountDetectorVO;
import com.serotonin.m2m2.web.mvc.spring.MangoRestSpringConfiguration;
import com.serotonin.util.SerializationHelper;
/**
 * Upgrade to add template system
 * 
 * @author Terry Packer
 *
 */
public class Upgrade12 extends DBUpgrade {

	private static final Log LOG = LogFactory.getLog(Upgrade12.class);
	
	private ObjectMapper mapper;
	
	public Upgrade12(){
		mapper = MangoRestSpringConfiguration.objectMapper;
	}

	
    @Override
    public void upgrade() throws Exception {
    	// get hash algorithm using the old default
    	String hashAlgorithm = Common.envProps.getString("security.hashAlgorithm", "SHA-1");

    	String[] mysqlScript = {
	        "ALTER TABLE users MODIFY password VARCHAR(255) NOT NULL;",
	        "UPDATE users SET password  = CONCAT('{" + hashAlgorithm + "}', password);",
	        
	        "ALTER TABLE eventHandlers ADD COLUMN eventHandlerType VARCHAR(40);",

	        "CREATE TABLE audit (id int NOT NULL auto_increment,typeName varchar(32) NOT NULL,alarmLevel int NOT NULL,userId int NOT NULL,changeType int NOT NULL,objectId int NOT NULL,ts bigint NOT NULL,context longtext, message varchar(255),PRIMARY KEY (id))engine=InnoDB;",
            "CREATE INDEX audit_performance1 ON audit (`ts` ASC);",

            "CREATE TABLE eventDetectors (id int NOT NULL auto_increment,xid varchar(50) NOT NULL,sourceTypeName varchar(32) NOT NULL, typeName varchar(32) NOT NULL,dataPointId int,data longtext NOT NULL,PRIMARY KEY (id))engine=InnoDB;",
            "ALTER TABLE eventDetectors ADD CONSTRAINT eventDetectorsUn1 UNIQUE (xid);",
            "ALTER TABLE eventDetectors ADD CONSTRAINT dataPointIdFk FOREIGN KEY (dataPointId) REFERENCES dataPoints(id);",
            
	    	"ALTER TABLE dataPoints ADD INDEX nameIndex (name ASC);",
	    	"ALTER TABLE dataPoints ADD INDEX deviceNameIndex (deviceName ASC);",
	    	"ALTER TABLE dataPoints ADD INDEX pointFolderIdIndex (pointFolderId ASC);",
	    	"ALTER TABLE dataPoints ADD INDEX dataSourceIdIndex (dataSourceId ASC);",
	    	"ALTER TABLE jsonData ADD COLUMN publicData char(1);",
	        "UPDATE jsonData SET publicData='N';",
	    };
	    String[] derbyScript = {
	        "ALTER TABLE users ALTER COLUMN password SET DATA TYPE VARCHAR(255);",
	        "UPDATE users SET password  = '{" + hashAlgorithm + "}' || password;",
	    
	    	"ALTER TABLE eventHandlers ADD COLUMN eventHandlerType VARCHAR(40);",
	    	
	    	"CREATE TABLE eventDetectors (id int NOT NULL generated by default as identity (start with 1, increment by 1),xid varchar(50) NOT NULL,sourceTypeName varchar(32) NOT NULL, typeName varchar(32) NOT NULL,dataPointId int,data clob NOT NULL);",
	    	"ALTER TABLE eventDetectors ADD CONSTRAINT eventDetectorsPk PRIMARY KEY (id);",
            "ALTER TABLE eventDetectors ADD CONSTRAINT eventDetectorsUn1 UNIQUE (xid);",
	    	"ALTER TABLE eventDetectors ADD CONSTRAINT dataPointIdFk FOREIGN KEY (dataPointId) REFERENCES dataPoints(id);",

            "CREATE TABLE audit (id int not null generated by default as identity (start with 1, increment by 1),typeName varchar(32) NOT NULL,alarmLevel int NOT NULL,userId int NOT NULL,changeType int NOT NULL,objectId int NOT NULL,ts bigint NOT NULL,context clob,message varchar(255));",
            "ALTER TABLE audit ADD CONSTRAINT auditPk PRIMARY KEY (id);",
            "CREATE INDEX audit_performance1 ON audit (ts ASC);",

	   		"CREATE INDEX nameIndex on dataPoints (name ASC);",
	   		"CREATE INDEX deviceNameIndex on dataPoints (deviceName ASC);",
	   		"CREATE INDEX pointFolderIdIndex on dataPoints (pointFolderId ASC);",
	   		"CREATE INDEX dataSourceIdIndex on dataPoints (dataSourceId ASC);",
	    	"ALTER TABLE jsonData ADD COLUMN publicData char(1);",
	        "UPDATE jsonData SET publicData='N';",
	    };    
	
	    String[] h2Script = {
	        "ALTER TABLE users ALTER COLUMN password VARCHAR(255) NOT NULL;",
	        "UPDATE users SET password  = CONCAT('{" + hashAlgorithm + "}', password);",
	        
	        "ALTER TABLE eventHandlers ADD COLUMN eventHandlerType VARCHAR(40);",

            "CREATE TABLE audit (id int NOT NULL auto_increment,typeName varchar(32) NOT NULL,alarmLevel int NOT NULL,userId int NOT NULL,changeType int NOT NULL, objectId int NOT NULL,ts bigint NOT NULL,context longtext,message varchar(255),PRIMARY KEY (id));",
            "CREATE INDEX audit_performance1 ON audit (`ts` ASC);",
            
            "CREATE TABLE eventDetectors (id int NOT NULL auto_increment,xid varchar(50) NOT NULL,sourceTypeName varchar(32) NOT NULL, typeName varchar(32) NOT NULL,dataPointId int,data longtext NOT NULL,PRIMARY KEY (id));",
            "ALTER TABLE eventDetectors ADD CONSTRAINT eventDetectorsUn1 UNIQUE (xid);",
            "ALTER TABLE eventDetectors ADD CONSTRAINT dataPointIdFk FOREIGN KEY (dataPointId) REFERENCES dataPoints(id);",
            
	    	"CREATE INDEX nameIndex on dataPoints (`name` ASC);",
	    	"CREATE INDEX deviceNameIndex on dataPoints (`deviceName` ASC);",
	    	"CREATE INDEX pointFolderIdIndex on dataPoints (`pointFolderId` ASC);",
	    	"CREATE INDEX dataSourceIdIndex on dataPoints (`dataSourceId` ASC);", 
	    	"ALTER TABLE jsonData ADD COLUMN publicData char(1);",
	        "UPDATE jsonData SET publicData='N';",
	    };
	    
	   	String[] mssqlScript = {
	        "ALTER TABLE users ALTER COLUMN password nvarchar(255) NOT NULL;",
	        "UPDATE users SET password  = CONCAT('{" + hashAlgorithm + "}', password);",
	        
	        "ALTER TABLE eventHandlers ADD COLUMN eventHandlerType nvarchar(40);",

            "CREATE TABLE eventDetectors (id int NOT NULL identity,xid nvarchar(50) NOT NULL,sourceTypeName nvarchar(32) NOT NULL,typeName nvarchar(32) NOT NULL,dataPointId int,data ntext NOT NULL, PRIMARY KEY (id));",
            "ALTER TABLE eventDetectors ADD CONSTRAINT eventDetectorsUn1 UNIQUE (xid);",
            "ALTER TABLE eventDetectors ADD CONSTRAINT dataPointIdFk FOREIGN KEY (dataPointId) REFERENCES dataPoints(id);",
            
            "CREATE TABLE audit (id int NOT NULL identity,typeName nvarchar(32) NOT NULL,alarmLevel int NOT NULL,userId int NOT NULL,changeType int NOT NULL, objectId int NOT NULL,ts bigint NOT NULL,context ntext,message nvarchar(255),PRIMARY KEY (id));",
            "CREATE INDEX audit_performance1 ON audit (`ts` ASC);",
	        
	   		"CREATE INDEX nameIndex on dataPoints (name ASC);",
	   		"CREATE INDEX deviceNameIndex on dataPoints (deviceName ASC);",
	   		"CREATE INDEX pointFolderIdIndex on dataPoints (pointFolderId ASC);",
	   		"CREATE INDEX dataSourceIdIndex on dataPoints (dataSourceId ASC);",
	    	"ALTER TABLE jsonData ADD COLUMN publicData char(1);",
	        "UPDATE jsonData SET publicData='N';",    
	    };
    
        // Run the script.
	   	OutputStream os = createUpdateLogOutputStream();
        Map<String, String[]> scripts = new HashMap<>();
        scripts.put(DatabaseProxy.DatabaseType.DERBY.name(), derbyScript);
        scripts.put(DatabaseProxy.DatabaseType.MYSQL.name(), mysqlScript);
        scripts.put(DatabaseProxy.DatabaseType.MSSQL.name(), mssqlScript);
        scripts.put(DatabaseProxy.DatabaseType.H2.name(), h2Script);
        scripts.put(DatabaseProxy.DatabaseType.POSTGRES.name(), mysqlScript);
        runScript(scripts, os);
        
        int upgradedEventHandlerCount = upgradeEventHandlers(os);
        String upgradedString = new String("Upgraded " + upgradedEventHandlerCount + " event handlers.\n");
        os.write(upgradedString.getBytes(Common.UTF8_CS));
        
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
        runScript(scripts, os);
        
        //Dump Audit Events into SQL File in Backup folder
        backupAuditEvents(os);
        
        //Remove audit events
        int removed = this.ejt.update("DELETE FROM events WHERE typeName=?", new Object[]{EventType.EventTypeNames.AUDIT});
        String removedString = new String("Deleted " + removed + " AUDIT events from the events table.\n");
        os.write(removedString.getBytes(Common.UTF8_CS));
        
        //Upgrade the Event Detectors
        int upgradedEventDetectorCount = upgradeEventDetectors(os);
        String upgradedED = new String("Upgraded " + upgradedEventDetectorCount + " event detectors.\n");
        os.write(upgradedED.getBytes(Common.UTF8_CS));

        //Drop the pointEventDetectors table.
        this.ejt.update("DROP TABLE pointEventDetectors");

    }

    private static final String EVENT_HANDLER_SELECT = "select id, xid, alias, data from eventHandlers ";

	@SuppressWarnings("deprecation")
    private int upgradeEventHandlers(OutputStream os){

		int upgraded = 0;
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
    		        upgraded++;
    		    break;
    			case EventHandlerVO.TYPE_PROCESS:
    		        ProcessEventHandlerVO processHandler = new ProcessEventHandlerVO();
    		        processHandler.setDefinition(ModuleRegistry.getEventHandlerDefinition(ProcessEventHandlerDefinition.TYPE_NAME));
    		        processHandler.setActiveProcessCommand(vo.getActiveProcessCommand());
    		        processHandler.setActiveProcessTimeout(vo.getActiveProcessTimeout());
    		        processHandler.setInactiveProcessCommand(vo.getInactiveProcessCommand());
    		        processHandler.setInactiveProcessTimeout(vo.getInactiveProcessTimeout());
    		        upgradeEventHandler(processHandler);
    		        upgraded++;
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
    		        upgraded++;
    			break;
    			default:
    				LOG.error("Unknown event detector type: " + vo.getHandlerType());
    				try{
    					os.write(new String("Unknown event detector type: " + vo.getHandlerType()).getBytes(Common.UTF8_CS));
    				}catch(IOException e2){
    	    			LOG.error("Unable to write to upgrade log.", e2);
    	    		}
    			break;
    		}
    	}
    	
    	return upgraded;
    }
    
	/**
	 * Upgrade a handler in the DB
	 * @param handler
	 */
    void upgradeEventHandler(AbstractEventHandlerVO<?> handler) {
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

    /**
     * Export all audit events prior to deleting from table
     * @return
     * @throws IOException 
     */
    private void backupAuditEvents(OutputStream os){

    	//Write them to disk
    	String backupLocation = SystemSettingsDao.getValue(SystemSettingsDao.BACKUP_FILE_LOCATION);
    	File backupFolder = new File(backupLocation);
    	boolean writable = true;
    	if(!backupFolder.exists())
    		writable = backupFolder.mkdir();
    	
    	if(!writable) {
    		LOG.error("Unable to backup audit events, backup folder doesn't exist and couldn't be created.");
    		return;
    	}
    	
		File backupFile = new File(backupLocation, "auditEventBackup.json");

    	EventRowCallbackHandler handler = new EventRowCallbackHandler(backupFile, os);
    	try{
	    	handler.open();
	    	this.ejt.query("SELECT subtypeName,typeRef1,typeRef2,activeTs,alarmLevel,message from events where typeName=?", new Object[]{EventType.EventTypeNames.AUDIT}, handler);
	    	handler.close();
    	}catch(IOException e){
    		LOG.error(e.getMessage(), e);
    		try{
    			os.write(e.getMessage().getBytes(Common.UTF8_CS));
    			os.write("\n".getBytes(Common.UTF8_CS));
    		}catch(IOException e2){
    			LOG.error("Unable to write to upgrade log.", e2);
    		}
    	}
        String result = new String("Backed up " + handler.count + " audit events into " + backupFile.getAbsolutePath() + "\n");
        try{
        	os.write(result.getBytes(Common.UTF8_CS));
        }catch(IOException e2){
			LOG.error("Unable to write to upgrade log.", e2);
		}
    }

    class EventRowCallbackHandler implements RowCallbackHandler{

    	int count = 0;
    	File backupFile;
    	JsonGenerator generator;
    	OutputStream os;
    	
    	public EventRowCallbackHandler(File backupFile, OutputStream os){
    		this.backupFile = backupFile;
    		this.os = os;
    	}
    	
		/* (non-Javadoc)
		 * @see org.springframework.jdbc.core.RowCallbackHandler#processRow(java.sql.ResultSet)
		 */
		@Override
		public void processRow(ResultSet rs) throws SQLException {
			count++;
			//Create the map
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("subtypeName", rs.getString(1));
			map.put("typeRef1", rs.getString(2));
			map.put("typeRef2", rs.getString(3));
			map.put("activeTs", rs.getLong(4));
			map.put("alarmLevel", rs.getInt(5));
			TranslatableMessage msg = readTranslatableMessage(rs, 6);
			if(msg != null)
				map.put("message",  msg.translate(Common.getTranslations()));
			
			//Write it out
			try {
				generator.writeStartObject();
				generator.writeObject(map);
				generator.writeEndObject();
			} catch (IOException e) {
				LOG.error(e.getMessage(), e);
				try{
					os.write(e.getMessage().getBytes(Common.UTF8_CS));
					os.write("\n".getBytes(Common.UTF8_CS));
				}catch(IOException e2){
					LOG.error("Unable to write to upgrade log.", e2);
				}
			}
			
		}
		
		void open() throws IOException{
    		JsonFactory f = new JsonFactory(mapper);
    		FileWriter writer = new FileWriter(this.backupFile);
			generator = f.createGenerator(writer);
			generator.writeStartArray();
		}
		
		void close() throws IOException{
			generator.writeEndArray();
			generator.close();
		}
    	
    }
    
	/**
	 * Upgrade the old detectors and move into new table
	 * @return
	 */
	private int upgradeEventDetectors(OutputStream os){
		
		//Extract them and put them into the new table
        List<AbstractEventDetectorVO<?>> detectors = this.ejt.query(
                "select id, xid, alias, dataPointId, detectorType, alarmLevel, stateLimit, duration, durationType, binaryState, "
                        + "  multistateState, changeCount, alphanumericState, weight from pointEventDetectors"
                        , new Object[] { }, new LegacyEventDetectorRowMapper(os));
		
        //Save the new ones
        for(AbstractEventDetectorVO<?> vo : detectors){
    		String jsonData = null;
    		try{ 
    			jsonData = EventDetectorDao.instance.writeValueAsString(vo);
    		}catch(JsonException | IOException e){
    			LOG.error(e.getMessage(), e);
    			try{
	    			os.write(e.getMessage().getBytes(Common.UTF8_CS));
	    			os.write("\n".getBytes(Common.UTF8_CS));
    			}catch(IOException e2){
    				LOG.error("Unable to write to upgrade log.", e2);
    			}
    		}
    		
    		this.ejt.doInsert("INSERT INTO eventDetectors (xid, sourceTypeName, typeName, dataPointId, data) values (?,?,?,?,?)", new Object[]{
    			vo.getXid(),
    			vo.getDetectorSourceType(),
    			vo.getDetectorType(),
    			vo.getSourceId(),
    			jsonData,
    		}, new int[]{Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.CLOB});
        }
        
        return detectors.size();
	}
	
    class LegacyEventDetectorRowMapper implements RowMapper<AbstractEventDetectorVO<?>> {

    	OutputStream os;
        public LegacyEventDetectorRowMapper(OutputStream os) {
        	this.os = os;
        }

        @SuppressWarnings("deprecation")
		@Override
        public AbstractEventDetectorVO<?> mapRow(ResultSet rs, int rowNum) throws SQLException {
            
            //Switch on the type
            switch(rs.getInt(5)){
            case PointEventDetectorVO.TYPE_ALPHANUMERIC_REGEX_STATE:
            	AlphanumericRegexStateDetectorVO arsd = (AlphanumericRegexStateDetectorVO) new AlphanumericRegexStateEventDetectorDefinition().baseCreateEventDetectorVO();
            	arsd.setXid(rs.getString(2));
            	arsd.setAlias(rs.getString(3));
            	arsd.setAlarmLevel(rs.getInt(5));
            	arsd.setSourceId(rs.getInt(4));
            	arsd.setDuration(rs.getInt(8));
            	arsd.setDurationType(rs.getInt(9));
            	arsd.setState(rs.getString(13));
            	return arsd;
            case PointEventDetectorVO.TYPE_ALPHANUMERIC_STATE:
            	AlphanumericStateDetectorVO asd = (AlphanumericStateDetectorVO) new AlphanumericStateEventDetectorDefinition().baseCreateEventDetectorVO();
            	asd.setXid(rs.getString(2));
            	asd.setAlias(rs.getString(3));
            	asd.setAlarmLevel(rs.getInt(5));
            	asd.setSourceId(rs.getInt(4));
            	asd.setDuration(rs.getInt(8));
            	asd.setDurationType(rs.getInt(9));
            	asd.setState(rs.getString(13));
            	return asd;
            case PointEventDetectorVO.TYPE_ANALOG_CHANGE:
            	AnalogChangeDetectorVO acd = (AnalogChangeDetectorVO)new AnalogChangeEventDetectorDefinition().baseCreateEventDetectorVO();
            	acd.setXid(rs.getString(2));
            	acd.setAlias(rs.getString(3));
            	acd.setAlarmLevel(rs.getInt(5));
            	acd.setSourceId(rs.getInt(4));
            	acd.setDuration(rs.getInt(8));
            	acd.setDurationType(rs.getInt(9));
            	acd.setLimit(rs.getDouble(7));
            	acd.setNotHigher(rs.getBoolean(10));
            	return acd;
            case PointEventDetectorVO.TYPE_ANALOG_HIGH_LIMIT:
            	AnalogHighLimitDetectorVO ahld = (AnalogHighLimitDetectorVO)new AnalogHighLimitEventDetectorDefinition().baseCreateEventDetectorVO();
            	ahld.setXid(rs.getString(2));
            	ahld.setAlias(rs.getString(3));
            	ahld.setAlarmLevel(rs.getInt(5));
            	ahld.setSourceId(rs.getInt(4));
            	ahld.setDuration(rs.getInt(8));
            	ahld.setDurationType(rs.getInt(9));
            	ahld.setLimit(rs.getDouble(7));
            	ahld.setResetLimit(rs.getDouble(14));
            	ahld.setUseResetLimit(rs.getInt(11) == 1);
            	ahld.setNotHigher(rs.getBoolean(10));
            	return ahld;
            case PointEventDetectorVO.TYPE_ANALOG_LOW_LIMIT:
            	AnalogLowLimitDetectorVO alld = (AnalogLowLimitDetectorVO)new AnalogLowLimitEventDetectorDefinition().baseCreateEventDetectorVO();
            	alld.setXid(rs.getString(2));
            	alld.setAlias(rs.getString(3));
            	alld.setAlarmLevel(rs.getInt(5));
            	alld.setSourceId(rs.getInt(4));
            	alld.setDuration(rs.getInt(8));
            	alld.setDurationType(rs.getInt(9));
            	alld.setLimit(rs.getDouble(7)); //stateLimit
            	alld.setResetLimit(rs.getDouble(14)); //weight
            	alld.setUseResetLimit(rs.getInt(11) == 1); //multistateState
            	alld.setNotLower(rs.getBoolean(10)); //binaryState
            	return alld;
            case PointEventDetectorVO.TYPE_ANALOG_RANGE:
            	AnalogRangeDetectorVO ard = (AnalogRangeDetectorVO)new AnalogRangeEventDetectorDefinition().baseCreateEventDetectorVO();
            	ard.setXid(rs.getString(2));
            	ard.setAlias(rs.getString(3));
            	ard.setAlarmLevel(rs.getInt(5));
            	ard.setSourceId(rs.getInt(4));
            	ard.setDuration(rs.getInt(8));
            	ard.setDurationType(rs.getInt(9));
            	ard.setHigh(rs.getDouble(7)); //stateLimit
            	ard.setLow(rs.getDouble(14)); //weight
            	ard.setWithinRange(rs.getBoolean(10)); //binaryState
            	return ard;
            case PointEventDetectorVO.TYPE_BINARY_STATE:
            	BinaryStateDetectorVO bsd = (BinaryStateDetectorVO)new BinaryStateEventDetectorDefinition().baseCreateEventDetectorVO();
            	bsd.setXid(rs.getString(2));
            	bsd.setAlias(rs.getString(3));
            	bsd.setAlarmLevel(rs.getInt(5));
            	bsd.setSourceId(rs.getInt(4));
            	bsd.setDuration(rs.getInt(8));
            	bsd.setDurationType(rs.getInt(9));
            	bsd.setState(rs.getBoolean(10)); //binaryState
            	return bsd;
            case PointEventDetectorVO.TYPE_MULTISTATE_STATE:
            	MultistateStateDetectorVO msd = (MultistateStateDetectorVO)new MultistateStateEventDetectorDefinition().baseCreateEventDetectorVO();
            	msd.setXid(rs.getString(2));
            	msd.setAlias(rs.getString(3));
            	msd.setAlarmLevel(rs.getInt(5));
            	msd.setSourceId(rs.getInt(4));
            	msd.setDuration(rs.getInt(8));
            	msd.setDurationType(rs.getInt(9));
            	msd.setState(rs.getInt(11)); //binaryState
            	return msd;
            case PointEventDetectorVO.TYPE_NEGATIVE_CUSUM:
            	NegativeCusumDetectorVO ncd = (NegativeCusumDetectorVO)new NegativeCusumEventDetectorDefinition().baseCreateEventDetectorVO();
            	ncd.setXid(rs.getString(2));
            	ncd.setAlias(rs.getString(3));
            	ncd.setAlarmLevel(rs.getInt(5));
            	ncd.setSourceId(rs.getInt(4));
            	ncd.setDuration(rs.getInt(8));
            	ncd.setDurationType(rs.getInt(9));
            	ncd.setLimit(rs.getDouble(7)); //stateLimit
            	ncd.setWeight(rs.getDouble(14)); //weight
            	return ncd;
            case PointEventDetectorVO.TYPE_NO_CHANGE:
            	NoChangeDetectorVO ncd2 = (NoChangeDetectorVO)new NoChangeEventDetectorDefinition().baseCreateEventDetectorVO();
            	ncd2.setXid(rs.getString(2));
            	ncd2.setAlias(rs.getString(3));
            	ncd2.setAlarmLevel(rs.getInt(5));
            	ncd2.setSourceId(rs.getInt(4));
            	ncd2.setDuration(rs.getInt(8));
            	ncd2.setDurationType(rs.getInt(9));
            	return ncd2;
            case PointEventDetectorVO.TYPE_NO_UPDATE:
            	NoUpdateDetectorVO nud = (NoUpdateDetectorVO)new NoUpdateEventDetectorDefinition().baseCreateEventDetectorVO();
            	nud.setXid(rs.getString(2));
            	nud.setAlias(rs.getString(3));
            	nud.setAlarmLevel(rs.getInt(5));
            	nud.setSourceId(rs.getInt(4));
            	nud.setDuration(rs.getInt(8));
            	nud.setDurationType(rs.getInt(9));
            	return nud;
            case PointEventDetectorVO.TYPE_POINT_CHANGE:
            	PointChangeDetectorVO pcd = (PointChangeDetectorVO)new PointChangeEventDetectorDefinition().baseCreateEventDetectorVO();
            	pcd.setXid(rs.getString(2));
            	pcd.setAlias(rs.getString(3));
            	pcd.setAlarmLevel(rs.getInt(5));
            	pcd.setSourceId(rs.getInt(4));
            	return pcd;
            case PointEventDetectorVO.TYPE_POSITIVE_CUSUM:
            	PositiveCusumDetectorVO pcd2 = (PositiveCusumDetectorVO)new PositiveCusumEventDetectorDefinition().baseCreateEventDetectorVO();
            	pcd2.setXid(rs.getString(2));
            	pcd2.setAlias(rs.getString(3));
            	pcd2.setAlarmLevel(rs.getInt(5));
            	pcd2.setSourceId(rs.getInt(4));
            	pcd2.setDuration(rs.getInt(8));
            	pcd2.setDurationType(rs.getInt(9));
            	pcd2.setLimit(rs.getDouble(7)); //stateLimit
            	pcd2.setWeight(rs.getDouble(14)); //weight
            	return pcd2;
            case PointEventDetectorVO.TYPE_SMOOTHNESS:
            	SmoothnessDetectorVO sd = (SmoothnessDetectorVO)new SmoothnessEventDetectorDefinition().baseCreateEventDetectorVO();
            	sd.setXid(rs.getString(2));
            	sd.setAlias(rs.getString(3));
            	sd.setAlarmLevel(rs.getInt(5));
            	sd.setSourceId(rs.getInt(4));
            	sd.setDuration(rs.getInt(8));
            	sd.setDurationType(rs.getInt(9));
            	sd.setLimit(rs.getDouble(7)); //stateLimit
            	sd.setBoxcar(rs.getInt(12)); //change count
            	return sd;
            case PointEventDetectorVO.TYPE_STATE_CHANGE_COUNT:
            	StateChangeCountDetectorVO scc = (StateChangeCountDetectorVO)new StateChangeCountEventDetectorDefinition().baseCreateEventDetectorVO();
            	scc.setXid(rs.getString(2));
            	scc.setAlias(rs.getString(3));
            	scc.setAlarmLevel(rs.getInt(5));
            	scc.setSourceId(rs.getInt(4));
            	scc.setDuration(rs.getInt(8));
            	scc.setDurationType(rs.getInt(9));
            	scc.setChangeCount(rs.getInt(12)); //change count
            	return scc;
            default:
            	//Not supported
            	LOG.warn("unable to convert event detector of type: " + rs.getInt(5) );
            	try{
            	os.write(new String("unable to convert event detector of type: " + rs.getInt(5) + "\n").getBytes(Common.UTF8_CS));
            	}catch(IOException e){
            		LOG.error("Unable to write to upgrade log.", e);
            	}
            	return null;
            }
        }
    }

    @Override
    protected String getNewSchemaVersion() {
        return "13";
    }
}
