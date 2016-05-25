/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.db.dao;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.Types;
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
import com.serotonin.m2m2.module.definitions.websocket.EventDetectorWebSocketDefinition;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.vo.event.detector.AbstractEventDetectorVO;

/**
 * @author Terry Packer
 *
 */
public class EventDetectorDao extends AbstractDao<AbstractEventDetectorVO<?>>{
	
	public static final EventDetectorDao instance = new EventDetectorDao();
	
	private EventDetectorDao(){
		super(EventDetectorWebSocketDefinition.handler, AuditEventType.TYPE_EVENT_DETECTOR, "edt", new String[0], null);
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
		
		return new Object[]{
			vo.getXid(),
			vo.getDetectorType(),
			vo.getSourceId(),
			jsonData,
		};
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#getPropertyTypeMap()
	 */
	@Override
	protected LinkedHashMap<String, Integer> getPropertyTypeMap() {
		LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>();
		map.put("id", Types.INTEGER);
		map.put("xid", Types.VARCHAR);
		map.put("typeName", Types.VARCHAR);
		map.put("sourceId", Types.INTEGER);
		map.put("data", Types.CLOB);
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
            ejt.update("delete from eventHandlers where eventTypeName=? and eventTypeRef1=? and eventTypeRef2=?",
                    new Object[] { vo.getEventType().getType(), vo.getSourceId(), vo.getId() });

            AuditEventType.raiseDeletedEvent(this.typeName, vo);
        }
    }
	/**
	 * Get all with given source id.
	 * Ordered by detector id.
	 * 
	 * @param sourceId
	 * @return
	 */
	public List<AbstractEventDetectorVO<?>> getWithSourceId(int sourceId){
		return query(SELECT_ALL + " WHERE sourceId=? ORDER BY id", new Object[]{sourceId}, new EventDetectorRowMapper());
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
	public int getId(String xid) {
		return queryForObject("SELECT id from " + this.tableName + " WHERE xid=?", new Object[]{xid}, Integer.class, -1);
	}

	/**
	 * Return only the source id from the table for a given row
	 * @param pedid
	 * @return
	 */
	public int getSourceId(int id) {
		return queryForObject("SELECT sourceId from " + this.tableName + " WHERE id=?", new Object[]{id}, Integer.class, -1);
	}
}
