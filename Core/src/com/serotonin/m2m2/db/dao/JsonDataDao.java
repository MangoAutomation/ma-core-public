/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.db.dao;

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
import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.pair.IntStringPair;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.vo.json.JsonDataVO;

/**
 * @author Terry Packer
 *
 */
@Repository()
public class JsonDataDao extends AbstractDao<JsonDataVO>{

    private static final LazyInitSupplier<JsonDataDao> springInstance = new LazyInitSupplier<>(() -> {
        Object o = Common.getRuntimeContext().getBean(JsonDataDao.class);
        if(o == null)
            throw new ShouldNeverHappenException("DAO not initialized in Spring Runtime Context");
        return (JsonDataDao)o;
    });

	/**
	 * @param handler
	 * @param typeName
	 */
	private JsonDataDao() {
		super(AuditEventType.TYPE_JSON_DATA, new TranslatableMessage("internal.monitor.JSON_DATA_COUNT"));
	}

    /**
     * Get cached instance from Spring Context
     * @return
     */
    public static JsonDataDao getInstance() {
        return springInstance.get();
    }
    
	@Override
	protected String getXidPrefix() {
		return JsonDataVO.XID_PREFIX;
	}

	@Override
	protected String getTableName() {
		return SchemaDefinition.JSON_DATA_TABLE;
	}

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

	@Override
	protected Map<String, IntStringPair> getPropertiesMap() {
		return new HashMap<String, IntStringPair>();
	}

	@Override
	public RowMapper<JsonDataVO> getRowMapper() {
		return new JsonDataRowMapper();
	}

	class JsonDataRowMapper implements RowMapper<JsonDataVO>{

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
