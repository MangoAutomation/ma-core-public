/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.db.dao;

import java.io.IOException;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.RowMapper;

import com.fasterxml.jackson.databind.JsonNode;
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
public class EventDetectorRowMapper<T extends AbstractEventDetectorVO> implements RowMapper<T>{

    protected final Log LOG = LogFactory.getLog(EventDetectorRowMapper.class);
    //First column in the query for the event detector columns
    protected int firstColumn;
    //Column offset for the sourceId to use, -1 means use definition mappings
    protected int sourceIdColumnOffset;
    protected ExtractJson<Clob, JsonNode> extractJson;

    public EventDetectorRowMapper(ExtractJson<Clob, JsonNode> extractJson){
        this(1, -1, extractJson);
    }

    /**
     *
     * @param firstColumn - First column of Event Detector columns in ResultSet
     * @param sourceIdColumnOffset - Offset from first column to where
     * 	the sourceId column is located < 0 means use definition and expect all sourceId columns in result
     * @param extractJson - method to extract a JsonNode from the Clob in the table
     */
    public EventDetectorRowMapper(int firstColumn, int sourceIdColumnOffset, ExtractJson<Clob, JsonNode> extractData){
        this.firstColumn = firstColumn;
        this.sourceIdColumnOffset = sourceIdColumnOffset;
        this.extractJson = extractData;
    }

    @Override
    public T mapRow(ResultSet rs, int rowNum)
            throws SQLException {
        String type = rs.getString(this.firstColumn + 3);
        EventDetectorDefinition<?> definition = ModuleRegistry.getEventDetectorDefinition(type);
        if(definition == null)
            throw new ShouldNeverHappenException("Event Detector defintion of type: " + rs.getString(this.firstColumn + 3) + " not found." );

        //Extract the source id
        int sourceIdColumnIndex;
        if(this.sourceIdColumnOffset < 0)
            sourceIdColumnIndex= this.firstColumn + 6 + EventDetectorDao.getInstance().getSourceIdIndex(definition.getSourceTypeName());
        else
            sourceIdColumnIndex = this.firstColumn + this.sourceIdColumnOffset;
        int sourceId = rs.getInt(sourceIdColumnIndex);

        T vo = createEventDetector(sourceId, definition);

        vo.setId(rs.getInt(this.firstColumn));
        vo.setXid(rs.getString(this.firstColumn + 1));
        vo.setData(this.extractJson.extract(rs.getClob(this.firstColumn + 4)));
        vo.setDefinition(definition);
        vo.setSourceId(sourceId);

        //Read Into Detector
        JsonTypeReader typeReader = new JsonTypeReader(rs.getString(this.firstColumn + 5));
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

    /**
     * @return
     */
    @SuppressWarnings("unchecked")
    protected T createEventDetector(int sourceId, EventDetectorDefinition<?> definition) {
        return (T) definition.baseCreateEventDetectorVO(sourceId);
    }

    @FunctionalInterface
    public interface ExtractJson<T, R> {

        /**
         * Applies this function to the given argument.
         *
         * @param t the function argument
         * @return the function result
         */
        R extract(T t) throws SQLException;
    }

}