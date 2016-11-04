/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.db.dao;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.RowMapper;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonTypeReader;
import com.serotonin.json.type.JsonValue;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.module.EventDetectorDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.vo.event.detector.AbstractEventDetectorVO;

/**
 * Helper class to potentially be used in Joins with event detector table.
 * 
 * The first column is 1 in the normal event detector query.
 * 
 * @author Terry Packer
 *
 */
public class EventDetectorRowMapper implements RowMapper<AbstractEventDetectorVO<?>>{

	private final Log LOG = LogFactory.getLog(EventDetectorRowMapper.class);
	private int firstColumn;
	private String tablePrefix;
	
	public EventDetectorRowMapper(int firstColumn, String tablePrefix){
		this.firstColumn = firstColumn;
		this.tablePrefix = tablePrefix + ".";
	}
	
	public EventDetectorRowMapper(){
		this(1, "edt");
	}
	/* (non-Javadoc)
	 * @see org.springframework.jdbc.core.RowMapper#mapRow(java.sql.ResultSet, int)
	 */
	@Override
	public AbstractEventDetectorVO<?> mapRow(ResultSet rs, int rowNum)
			throws SQLException {
		
		EventDetectorDefinition definition = ModuleRegistry.getEventDetectorDefinition(rs.getString(this.firstColumn + 3));
		if(definition == null)
			throw new ShouldNeverHappenException("Event Detector defintion of type: " + rs.getString(this.firstColumn + 3) + " not found." );
		
		AbstractEventDetectorVO<?> vo = definition.baseCreateEventDetectorVO();
		vo.setId(rs.getInt(this.firstColumn));
		vo.setXid(rs.getString(this.firstColumn + 1));
		vo.setDefinition(definition);
		//Read Into Detector
		JsonTypeReader typeReader = new JsonTypeReader(rs.getString(this.firstColumn + 4));
		try {
            JsonValue value = typeReader.read();
            JsonObject root = value.toJsonObject();
            JsonReader reader = new JsonReader(Common.JSON_CONTEXT, root);
            reader.readInto(vo);
        }
        catch (ClassCastException | IOException | JsonException e) {
            LOG.error(e.getMessage(), e);
        }
		vo.setSourceId(rs.getInt(this.tablePrefix + definition.getSourceIdColumnName()));
        
		return vo;
	}
	
}