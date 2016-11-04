/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.db.dao;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.RowMapper;

import com.serotonin.db.pair.IntStringPair;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonWriter;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonTypeReader;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.vo.event.audit.AuditEventInstanceVO;

/**
 * @author Terry Packer
 *
 */
public class AuditEventDao extends AbstractBasicDao<AuditEventInstanceVO>{

	public static final AuditEventDao instance = new AuditEventDao();
	
	/**
	 * @param tablePrefix
	 * @param extraProperties
	 * @param extraSQL
	 */
	private AuditEventDao() {
		super(ModuleRegistry.getWebSocketHandlerDefinition("AUDIT"), "aud", new String[0]);
	}
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#getTableName()
	 */
	@Override
	protected String getTableName() {
		return SchemaDefinition.AUDIT_TABLE;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#voToObjectArray(java.lang.Object)
	 */
	@Override
	protected Object[] voToObjectArray(AuditEventInstanceVO vo) {
		String jsonData = null;
		try{ 
			jsonData = writeValueAsString(vo.getContext());

		}catch(JsonException | IOException e){
			LOG.error(e.getMessage(), e);
		}
		
		return new Object[]{
			vo.getTypeName(),
			vo.getAlarmLevel(),
			vo.getUserId(),
			vo.getChangeType(),
			vo.getObjectId(),
			vo.getTimestamp(),
			jsonData,
			writeTranslatableMessage(vo.getMessage())
		};
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#getPropertyTypeMap()
	 */
	@Override
	protected LinkedHashMap<String, Integer> getPropertyTypeMap() {
		LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>();
		map.put("id", Types.INTEGER);
		map.put("typeName", Types.VARCHAR);
		map.put("alarmLevel", Types.INTEGER);
		map.put("userId", Types.INTEGER);
		map.put("changeType", Types.INTEGER);
		map.put("objectId", Types.INTEGER);
		map.put("ts", Types.BIGINT);
		map.put("context", Types.CLOB);
		map.put("message", Types.VARCHAR);
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
	public RowMapper<AuditEventInstanceVO> getRowMapper() {
		return new AuditEventInstanceRowMapper();
	}

	class AuditEventInstanceRowMapper implements RowMapper<AuditEventInstanceVO>{

		/* (non-Javadoc)
		 * @see org.springframework.jdbc.core.RowMapper#mapRow(java.sql.ResultSet, int)
		 */
		@Override
		public AuditEventInstanceVO mapRow(ResultSet rs, int rowNum)
				throws SQLException {
			int i=0;
			AuditEventInstanceVO vo = new AuditEventInstanceVO();
			vo.setId(rs.getInt(++i));
			vo.setTypeName(rs.getString(++i));
			vo.setAlarmLevel(rs.getInt(++i));
			vo.setUserId(rs.getInt(++i));
			vo.setChangeType(rs.getInt(++i));
			vo.setObjectId(rs.getInt(++i));
			vo.setTimestamp(rs.getLong(++i));
			try {
				vo.setContext(readValueFromString(rs.getString(++i)));
			} catch (IOException | JsonException e) {
				LOG.error(e.getMessage(), e);
			}
			vo.setMessage(readTranslatableMessage(rs, ++i));
			return vo;
		}
		
	}
	
	public JsonObject readValueFromString(String json) throws JsonException, IOException {
		JsonTypeReader reader = new JsonTypeReader(json);
		return (JsonObject)reader.read();
	}
	
	public String writeValueAsString(JsonObject value) throws JsonException, IOException {
		StringWriter stringWriter = new StringWriter();
		JsonWriter writer = new JsonWriter(Common.JSON_CONTEXT, stringWriter);
        writer.writeObject(value);
        return stringWriter.toString();

	}

	/**
	 * Get the audit trail in time ascending order for this object
	 * 
	 * @param id
	 * @return
	 */
	public List<AuditEventInstanceVO> getAllForObject(String typeName, int id) {
		return query(SELECT_ALL + " WHERE typeName=? AND objectId=? ORDER BY ts ASC", new Object[]{typeName, id}, getRowMapper());
	}
	
}
