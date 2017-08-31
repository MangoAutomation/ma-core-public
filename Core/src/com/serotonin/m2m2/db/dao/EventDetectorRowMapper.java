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
	//First column in the query for the event detector columns
	private int firstColumn;
	//Column offset for the sourceId to use, -1 means use definition mappings
	private int sourceIdColumnOffset;
	
	/**
	 * 
	 * @param firstColumn - First column of Event Detector columns in ResultSet
	 * @param sourceIdColumnOffset - Offset from first column to where 
	 * 	the sourceId column is located < 0 means use definition and expect all sourceId columns in result
	 */
	public EventDetectorRowMapper(int firstColumn, int sourceIdColumnOffset){
		this.firstColumn = firstColumn;
		this.sourceIdColumnOffset = sourceIdColumnOffset;
	}
	
	public EventDetectorRowMapper(){
		this(1, -1);
	}
	/* (non-Javadoc)
	 * @see org.springframework.jdbc.core.RowMapper#mapRow(java.sql.ResultSet, int)
	 */
	@Override
	public AbstractEventDetectorVO<?> mapRow(ResultSet rs, int rowNum)
			throws SQLException {
		
		EventDetectorDefinition<?> definition = ModuleRegistry.getEventDetectorDefinition(rs.getString(this.firstColumn + 3));
		if(definition == null)
			throw new ShouldNeverHappenException("Event Detector defintion of type: " + rs.getString(this.firstColumn + 3) + " not found." );
		
		AbstractEventDetectorVO<?> vo = definition.baseCreateEventDetectorVO();
		vo.setId(rs.getInt(this.firstColumn));
		vo.setXid(rs.getString(this.firstColumn + 1));
		vo.setDefinition(definition);
		
		//Extract the source id
        int sourceIdColumnIndex;
        if(this.sourceIdColumnOffset < 0)
            sourceIdColumnIndex= this.firstColumn + 5 + EventDetectorDao.instance.getSourceIdIndex(definition.getSourceTypeName());
        else
            sourceIdColumnIndex = this.firstColumn + this.sourceIdColumnOffset;
        vo.setSourceId(rs.getInt(sourceIdColumnIndex));
		
		//Read Into Detector
		JsonTypeReader typeReader = new JsonTypeReader(rs.getString(this.firstColumn + 4));
		try {
            JsonValue value = typeReader.read();
            JsonObject root = value.toJsonObject();
            JsonReader reader = new JsonReader(Common.JSON_CONTEXT, root);
            root.remove("handlers");
            reader.readInto(vo);
        }
        catch (ClassCastException | IOException | JsonException e) {
            LOG.error(e.getMessage(), e);
        }
        
		return vo;
	}
	
}