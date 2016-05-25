/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.db.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.RowMapper;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.pair.IntStringPair;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.definitions.websocket.TemplateWebSocketDefinition;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.vo.template.BaseTemplateVO;
import com.serotonin.m2m2.vo.template.DataPointPropertiesTemplateDefinition;
import com.serotonin.m2m2.vo.template.DataPointPropertiesTemplateVO;
import com.serotonin.util.SerializationHelper;

/**
 * Class to manage Database access for templates
 * 
 * @author Terry Packer
 *
 */
public class TemplateDao extends AbstractDao<BaseTemplateVO<?>> {

	private static final String TEMPLATE_SELECT = "SELECT id, xid, name, templateType, readPermission, setPermission, data FROM templates ";
	public static final TemplateDao instance = new TemplateDao();
	
	/**
	 * @param typeName
	 */
	protected TemplateDao() {
		super(TemplateWebSocketDefinition.handler, AuditEventType.TYPE_TEMPLATE, "t", new String[]{}, new String());
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractDao#getTableName()
	 */
	@Override
	protected String getTableName() {
		return SchemaDefinition.TEMPLATES_TABLE;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractDao#getXidPrefix()
	 */
	@Override
	protected String getXidPrefix() {
		return BaseTemplateVO.XID_PREFIX;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractDao#voToObjectArray(com.serotonin.m2m2.vo.AbstractVO)
	 */
	@Override
	protected Object[] voToObjectArray(BaseTemplateVO<?> vo) {
		return new Object[]{
				vo.getXid(),
				vo.getName(),
				vo.getDefinition().getTemplateTypeName(),
				vo.getReadPermission(),
				vo.getSetPermission(),
				SerializationHelper.writeObject(vo)
		};

	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractDao#getNewVo()
	 */
	@Override
	public BaseTemplateVO<?> getNewVo() {
		throw new ShouldNeverHappenException("Unable to create generic template, a type is required");
	}


   
   
   
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#getPropertiesMap()
	 */
	@Override
	protected Map<String, IntStringPair> getPropertiesMap() {
		return new HashMap<>();
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#getRowMapper()
	 */
	@Override
	public RowMapper<BaseTemplateVO<?>> getRowMapper() {
		return new TemplateRowMapper();
	}

	class TemplateRowMapper implements RowMapper<BaseTemplateVO<?>>{

		/* (non-Javadoc)
		 * @see org.springframework.jdbc.core.RowMapper#mapRow(java.sql.ResultSet, int)
		 */
		@Override
		public BaseTemplateVO<?> mapRow(ResultSet rs, int rowNum)
				throws SQLException {
			BaseTemplateVO<?> template = (BaseTemplateVO<?>)SerializationHelper.readObject(rs.getBinaryStream(7));
			template.setId(rs.getInt(1));
			template.setXid(rs.getString(2));
			template.setName(rs.getString(3));
			template.setDefinition(ModuleRegistry.getTemplateDefinition(rs.getString(4)));
			template.setReadPermission(rs.getString(5));
			template.setSetPermission(rs.getString(6));
			
			return template;
					
		}
		
	}

	/* Data Point Properties Template Queries */
	
	/**
	 * Get all templates for a given data type
	 * 
	 * @param dataTypeId
	 * @return
	 */
	public List<DataPointPropertiesTemplateVO> getDataPointTemplatesByDataTypeId(
			int dataTypeId) {
		List<DataPointPropertiesTemplateVO> allDataPointTemplates = this.getAllDataPointTemplates();
		List<DataPointPropertiesTemplateVO> templates = new ArrayList<DataPointPropertiesTemplateVO>();
		for(DataPointPropertiesTemplateVO dataPointTemplate : allDataPointTemplates ){
			if(dataPointTemplate.getDataTypeId() == dataTypeId)
				templates.add(dataPointTemplate);
		}
		return templates;
		
	}

	/**
	 * Get all Data Point Templates
	 * @return
	 */
	public List<DataPointPropertiesTemplateVO> getAllDataPointTemplates() {
		return this.query(TEMPLATE_SELECT + " WHERE templateType = ?", 
				new Object[]{DataPointPropertiesTemplateDefinition.TEMPLATE_TYPE}, 
				new DataPointPropertiesTemplateRowMapper());
	}


	/**
	 * Get the default Template for a given Data Type
	 * @param dataTypeId
	 * @return Template if there is one else NULL
	 */
	public DataPointPropertiesTemplateVO getDefaultDataPointTemplate(
			int dataTypeId) {
		List<DataPointPropertiesTemplateVO> allDataPointTemplates = this.getAllDataPointTemplates();
		for(DataPointPropertiesTemplateVO dataPointTemplate : allDataPointTemplates ){
			if((dataPointTemplate.getDataTypeId() == dataTypeId)&&(dataPointTemplate.isDefaultTemplate()))
				return dataPointTemplate;
		}
		return null;

	}
	
	class DataPointPropertiesTemplateRowMapper implements RowMapper<DataPointPropertiesTemplateVO>{

		/* (non-Javadoc)
		 * @see org.springframework.jdbc.core.RowMapper#mapRow(java.sql.ResultSet, int)
		 */
		@Override
		public DataPointPropertiesTemplateVO mapRow(ResultSet rs, int rowNum)
				throws SQLException {
			DataPointPropertiesTemplateVO template = (DataPointPropertiesTemplateVO)SerializationHelper.readObject(rs.getBinaryStream(7));
			template.setId(rs.getInt(1));
			template.setXid(rs.getString(2));
			template.setName(rs.getString(3));
			template.setDefinition(ModuleRegistry.getTemplateDefinition(rs.getString(4)));
			template.setReadPermission(rs.getString(5));
			template.setSetPermission(rs.getString(6));
			
			return template;
		}
		
	}

	/**
	 * @param templateTypeName
	 */
	public void deleteTemplateType(String templateTypeName) {
		//TODO Need to delete the templates of this type from the table
		// should be done after all other references are fixed
		throw new ShouldNeverHappenException("Unimplemented!");
		
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
		map.put("templateType", Types.VARCHAR);
		map.put("readPermission", Types.VARCHAR);
		map.put("setPermission", Types.VARCHAR);
		map.put("data", Types.BINARY);
		
		return map;
	}

}
