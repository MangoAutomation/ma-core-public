/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.db.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.RowMapper;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.vo.template.BaseTemplateVO;
import com.serotonin.m2m2.vo.template.DataPointPropertiesTemplateVO;
import com.serotonin.util.SerializationHelper;

/**
 * Class to manage Database access for templates
 * 
 * @author Terry Packer
 *
 */
public class TemplateDao extends AbstractDao<BaseTemplateVO<?>> {

	public static final TemplateDao instance = new TemplateDao();
	
	/**
	 * @param typeName
	 */
	protected TemplateDao() {
		super(AuditEventType.TYPE_TEMPLATE, "t", new String[]{}, new String());
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
				vo.getTemplateType(),
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
	 * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#getProperties()
	 */
	@Override
	protected List<String> getProperties() {
		return Arrays.asList(
				"id", 
				"xid",
				"name",
				"templateType",
				"readPermission",
				"setPermission",
				"data");
	}

   @Override
    protected List<Integer> getPropertyTypes() {
        return Arrays.asList(
        		//Leaving out the ID
        		Types.VARCHAR,
        		Types.VARCHAR,
        		Types.VARCHAR,
        		Types.VARCHAR,
        		Types.VARCHAR,
        		Types.BINARY
        	);
   }
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#getPropertiesMap()
	 */
	@Override
	protected Map<String, String> getPropertiesMap() {
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
			//Skipping Template Type as that is inherent to the template object
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
		return this.query("SELECT id, xid, name, templateType, readPermission, setPermission, data FROM templates WHERE templateType = ?", 
				new Object[]{DataPointPropertiesTemplateVO.TEMPLATE_TYPE}, 
				new DataPointPropertiesTemplateRowMapper());
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
			//Skipping Template Type as that is inherent to the template object
			template.setReadPermission(rs.getString(5));
			template.setSetPermission(rs.getString(6));
			
			return template;
		}
		
	}

}
