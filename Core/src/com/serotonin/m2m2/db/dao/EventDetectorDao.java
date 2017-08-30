/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.db.dao;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.Types;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.RowMapper;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.pair.IntStringPair;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonWriter;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonTypeReader;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.EventDetectorDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.vo.event.EventTypeVO;
import com.serotonin.m2m2.vo.event.detector.AbstractEventDetectorVO;

/**
 * @author Terry Packer
 *
 */
public class EventDetectorDao extends AbstractDao<AbstractEventDetectorVO<?>>{
	
	public static final EventDetectorDao instance = new EventDetectorDao();
	
	/* Map of Source Type to Source ID Column Names */
	private LinkedHashMap<String, String> sourceTypeToColumnNameMap;
	
	private EventDetectorDao(){
		super(ModuleRegistry.getWebSocketHandlerDefinition("EVENT_DETECTOR"),
				AuditEventType.TYPE_EVENT_DETECTOR, 
				"edt",
				new String[0],
				false,
				new TranslatableMessage("internal.monitor.EVENT_DETECTOR_COUNT"));
	}
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#getTableName()
	 */
	@Override
	protected String getTableName() {
		return SchemaDefinition.EVENT_DETECTOR_TABLE;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#voToObjectArray(java.lang.Object)
	 */
	@Override
	protected Object[] voToObjectArray(AbstractEventDetectorVO<?> vo) {
		String jsonData = null;
		try{ 
			jsonData = writeValueAsString(vo);
		}catch(JsonException | IOException e){
			LOG.error(e.getMessage(), e);
		}
		
		//Find the index of our sourceIdColumn
		int sourceIdIndex = getSourceIdIndex(vo.getDefinition().getSourceTypeName());

		Object[] o = new Object[4 + this.sourceTypeToColumnNameMap.size()];
		o[0] = vo.getXid();
		o[1] = vo.getDetectorSourceType();
		o[2] = vo.getDetectorType();
		o[3] = jsonData;
		o[4 + sourceIdIndex] = vo.getSourceId();
		return o;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#getPropertyTypeMap()
	 */
	@Override
	protected LinkedHashMap<String, Integer> getPropertyTypeMap() {
		LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>();
		map.put("id", Types.INTEGER);
		map.put("xid", Types.VARCHAR);
		map.put("sourceTypeName", Types.VARCHAR);
		map.put("typeName", Types.VARCHAR);
		map.put("data", Types.CLOB);
		
		//Build our ordered column set from the Module Registry
		List<EventDetectorDefinition<?>> defs = ModuleRegistry.getEventDetectorDefinitions();
		this.sourceTypeToColumnNameMap = new LinkedHashMap<String, String>(defs.size());
		for(EventDetectorDefinition<?> def : defs){
			this.sourceTypeToColumnNameMap.put(def.getSourceTypeName(), this.TABLE_PREFIX + "." + def.getSourceIdColumnName());
			map.put(def.getSourceIdColumnName(), Types.INTEGER);
		}

		return map;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#getPropertiesMap()
	 */
	@Override
	protected Map<String, IntStringPair> getPropertiesMap() {
		return null;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#getRowMapper()
	 */
	@Override
	public RowMapper<AbstractEventDetectorVO<?>> getRowMapper() {
		return new EventDetectorRowMapper();
	}
	
	
	
	public JsonObject readValueFromString(String json) throws JsonException, IOException {
		JsonTypeReader reader = new JsonTypeReader(json);
		
		return (JsonObject)reader.read();
	}
	
	public String writeValueAsString(AbstractEventDetectorVO<?> value) throws JsonException, IOException {
		StringWriter stringWriter = new StringWriter();
		JsonWriter writer = new JsonWriter(Common.JSON_CONTEXT, stringWriter);
        writer.writeObject(value);
        return stringWriter.toString();

	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractDao#getXidPrefix()
	 */
	@Override
	protected String getXidPrefix() {
		return AbstractEventDetectorVO.XID_PREFIX; 
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractDao#getNewVo()
	 */
	@Override
	public AbstractEventDetectorVO<?> getNewVo() {
        throw new ShouldNeverHappenException("Unable to create generic event detector, must supply a type");
	}
	
    @Override
    public void delete(AbstractEventDetectorVO<?> vo, String initiatorId) {
        if (vo != null) {
            super.delete(vo, initiatorId);
            //Also update the Event Handlers
            ejt.update("delete from eventHandlersMapping where eventTypeName=? and eventTypeRef1=? and eventTypeRef2=?",
                    new Object[] { vo.getEventType().getType(), vo.getSourceId(), vo.getId() });

            AuditEventType.raiseDeletedEvent(this.typeName, vo);
        }
    }
    
    @Override
    public void save(AbstractEventDetectorVO<?> vo, String initiatorId) {
        if (vo.getId() <= Common.NEW_ID) {
            insert(vo, initiatorId);
        }
        else {
            update(vo, initiatorId);
        }
        
        if(vo.getAddedEventHandlers() != null) {
            EventTypeVO et = vo.getEventType();
            for(String xid : vo.getAddedEventHandlers())
                EventHandlerDao.instance.addEventHandlerMappingIfMissing(xid, et);
        }
    }
	/**
	 * Get all with given source id.
	 * Ordered by detector id.
	 * 
	 * @param sourceId
	 * @return
	 */
	public List<AbstractEventDetectorVO<?>> getWithSourceId(String sourceType, int sourceId){
		String sourceIdColumnName = getSourceIdColumnName(sourceType);
		return query(SELECT_ALL + " WHERE " + sourceIdColumnName +  "=? ORDER BY id", new Object[]{sourceId}, new EventDetectorRowMapper());
	}

	/**
	 * Get the XID for a given row
	 * 
	 * @param id
	 * @return
	 */
	public String getXid(int id) {
		return queryForObject("SELECT xid from " + this.tableName + " WHERE id=?", new Object[]{id}, String.class, null);
	}

	/**
	 * Get the id for a given row
	 * @param xid
	 * @return
	 */
	public int getId(String xid, int dpId) {
		return queryForObject("SELECT id from " + this.tableName + " WHERE xid=? AND dataPointId=?", new Object[]{xid, dpId}, Integer.class, -1);
	}

	/**
	 * Return only the source id from the table for a given row
	 * @param pedid
	 * @return
	 */
	public int getSourceId(int id, String sourceType) {
		String sourceIdColumn = getSourceIdColumnName(sourceType);
		return queryForObject("SELECT " + sourceIdColumn + " from " + this.tableName + "AS " + this.tablePrefix + " WHERE id=?", new Object[]{id}, Integer.class, -1);
	}
	
	@Override
	public AbstractEventDetectorVO<?> getByXid(String xid){
		throw new ShouldNeverHappenException("Not possible as XIDs are not unique");
	}

	/**
	 * Get an Event Detector based on sourceId and Xid uniqueness
	 * @param xid
	 * @param sourceType
	 * @param sourceId
	 * @return
	 */
	public AbstractEventDetectorVO<?> getByXid(String xid, String sourceType, int sourceId){
		String sourceIdColumn = getSourceIdColumnName(sourceType);
		return queryForObject("SELECT " + sourceIdColumn + " from " + this.tableName + " AS  "  + this.TABLE_PREFIX +  " WHERE xid=? AND " + sourceIdColumn + "=?", new Object[]{xid, sourceId}, getRowMapper(), null);
	}
	
	/**
	 * Get the column name for the source id using the source type
	 * @param sourceType
	 * @return
	 */
	public String getSourceIdColumnName(String sourceType){
		String columnName = this.sourceTypeToColumnNameMap.get(sourceType);
		if(columnName == null)
			throw new ShouldNeverHappenException("Unknown Detector Source Type: " + sourceType);
		else
			return columnName;
	}
	
	/**
	 * Get the index of the source id column in the result set returned from a Select
	 * @param sourceType
	 * @return
	 */
	public int getSourceIdIndex(String sourceType){
		int index = 0;
		Iterator<String> it = this.sourceTypeToColumnNameMap.keySet().iterator();
		while(it.hasNext()){
			if(it.next().equals(sourceType))
				break;
			index++;
		}
		
		return index;
	}
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractDao#isXidUnique(java.lang.String, int)
	 */
	@Override
	public boolean isXidUnique(String xid, int excludeId) {
		throw new ShouldNeverHappenException("XIDs are not unique without a source ID to compare.");
	}

	/**
	 * This method diverges with the normal uniqueness checks 
	 * because it requires the sourceID 
	 */
	public boolean isXidUnique(String xid, int id, String sourceType, int sourceId) {
		String sourceIdColumn = getSourceIdColumnName(sourceType);

		 return ejt.queryForInt("select count(*) from " + tableName + " AS " + this.TABLE_PREFIX + " where xid=? AND id<>? AND " + sourceIdColumn + "=?" , new Object[] { xid, id,
	                sourceId }, 0) == 0;
	}
}
