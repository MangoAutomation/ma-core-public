/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.spring.dao;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.serotonin.db.pair.IntStringPair;
import com.serotonin.m2m2.db.dao.AbstractDao;
import com.serotonin.m2m2.db.dao.SchemaDefinition;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.vo.json.JsonDataVO;

/**
 * @author Terry Packer
 *
 */
@Repository("jsonDataDao")
public class JsonDataDao extends AbstractDao<JsonDataVO>{

    @Deprecated
	public static JsonDataDao instance;
	
	/**
	 * @param handler
	 * @param typeName
	 */
	private JsonDataDao() {
		super(AuditEventType.TYPE_JSON_DATA, new TranslatableMessage("internal.monitor.JSON_DATA_COUNT"));
		instance = this;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractDao#getXidPrefix()
	 */
	@Override
	protected String getXidPrefix() {
		return JsonDataVO.XID_PREFIX;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractDao#getNewVo()
	 */
	@Override
	public JsonDataVO getNewVo() {
		return new JsonDataVO();
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#getTableName()
	 */
	@Override
	protected String getTableName() {
		return SchemaDefinition.JSON_DATA_TABLE;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#voToObjectArray(java.lang.Object)
	 */
	@Override
	protected Object[] voToObjectArray(JsonDataVO vo) {
		String jsonData = null;
		try{ 
			jsonData = writeValueAsString(vo.getJsonData());
		}catch(JsonProcessingException e){
			LOG.error(e.getMessage(), e);
		}

		return new Object[]{
			vo.getXid(),
			vo.getName(),
			vo.getReadPermission(),
			vo.getEditPermission(),
			boolToChar(vo.isPublicData()),
			jsonData
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
		map.put("name", Types.VARCHAR);
		map.put("readPermission", Types.VARCHAR);
		map.put("editPermission", Types.VARCHAR);
		map.put("publicData", Types.CHAR);
		map.put("data", Types.CLOB);
		return map;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#getPropertiesMap()
	 */
	@Override
	protected Map<String, IntStringPair> getPropertiesMap() {
		return new HashMap<String, IntStringPair>();
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#getRowMapper()
	 */
	@Override
	public RowMapper<JsonDataVO> getRowMapper() {
		return new JsonDataRowMapper();
	}

	class JsonDataRowMapper implements RowMapper<JsonDataVO>{

		/* (non-Javadoc)
		 * @see org.springframework.jdbc.core.RowMapper#mapRow(java.sql.ResultSet, int)
		 */
		@Override
		public JsonDataVO mapRow(ResultSet rs, int rowNum)
				throws SQLException {
			int i=0;
			JsonDataVO vo = new JsonDataVO();
			vo.setId(rs.getInt(++i));
			vo.setXid(rs.getString(++i));
			vo.setName(rs.getString(++i));
			vo.setReadPermission(rs.getString(++i));
			vo.setEditPermission(rs.getString(++i));
			vo.setPublicData(charToBool(rs.getString(++i)));
			
			//Read the data
			try{
				vo.setJsonData(getObjectReader(Object.class).readTree(rs.getClob(++i).getCharacterStream()));
			}catch(Exception e){
				LOG.error(e.getMessage(), e);
			}
			
			return vo;
		}
	}
	
	/**
	 * 
	 * @param json
	 * @return
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public JsonNode readValueFromString(String json) throws JsonParseException, JsonMappingException, IOException{
		return getObjectReader(JsonNode.class).readTree(json);
	}
	
	public String writeValueAsString(Object value) throws JsonProcessingException{
		return getObjectWriter(Object.class).writeValueAsString(value);
	}
}
