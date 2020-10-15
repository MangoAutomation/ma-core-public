/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.db.dao;

import java.io.IOException;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jooq.Field;
import org.springframework.jdbc.core.RowMapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.db.EventDetectorTableDefinition;
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
    protected final int firstColumn;
    protected final ExtractJson<Clob, JsonNode> extractJson;
    protected final EventDetectorDao dao;

    protected int xidIndex;
    protected int sourceTypeNameIndex;
    protected int typeNameIndex;
    protected int jsonDataIndex;
    protected int dataIndex;
    protected int readPermissionIndex;
    protected int editPermissionIndex;

    public EventDetectorRowMapper(ExtractJson<Clob, JsonNode> extractJson, EventDetectorDao dao){
        this(1, extractJson, dao);
    }

    /**
     *
     * @param firstColumn - First column of Event Detector columns in ResultSet
     * @param extractJson - method to extract a JsonNode from the Clob in the table
     */
    public EventDetectorRowMapper(int firstColumn, ExtractJson<Clob, JsonNode> extractData, EventDetectorDao dao){
        this.firstColumn = firstColumn;
        this.extractJson = extractData;
        this.dao = dao;
        computeColumnNumbers();
    }

    protected void computeColumnNumbers() {
        int index = this.firstColumn;
        List<Field<?>> fields = dao.getTable().getSelectFields();

        for(Field<?> selectField : fields) {
            switch(selectField.getName()) {
                case "xid":
                    this.xidIndex = index;
                    break;
                case EventDetectorTableDefinition.SOURCE_TYPE_NAME_NAME:
                    this.sourceTypeNameIndex = index;
                    break;
                case EventDetectorTableDefinition.TYPE_NAME_NAME:
                    this.typeNameIndex = index;
                    break;
                case EventDetectorTableDefinition.JSON_DATA_NAME:
                    this.jsonDataIndex = index;
                    break;
                case EventDetectorTableDefinition.DATA_NAME:
                    this.dataIndex = index;
                    break;
                case EventDetectorTableDefinition.READ_PERMISSION_NAME:
                    this.readPermissionIndex = index;
                    break;
                case EventDetectorTableDefinition.EDIT_PERMISSION_NAME:
                    this.editPermissionIndex = index;
                    break;
            }
            index++;
        }
    }

    @Override
    public T mapRow(ResultSet rs, int rowNum)
            throws SQLException {
        String type = rs.getString(typeNameIndex);
        EventDetectorDefinition<?> definition = ModuleRegistry.getEventDetectorDefinition(type);
        if(definition == null)
            throw new ShouldNeverHappenException("Event Detector defintion of type: " + rs.getString(typeNameIndex) + " not found." );

        //Extract the source id
        int sourceIdColumnIndex;
        //Compute the index of this source id
        Field<Integer> sourceIdField = dao.getSourceIdColumnName(definition.getSourceTypeName());
        sourceIdColumnIndex = this.firstColumn;
        for(Field<?> selectField : dao.getTable().getSelectFields()) {
            if(selectField.getName().equals(sourceIdField.getName())) {
                break;
            }
            sourceIdColumnIndex++;
        }
        int sourceId = rs.getInt(sourceIdColumnIndex);

        T vo = createEventDetector(sourceId, definition);

        vo.setId(rs.getInt(this.firstColumn));
        vo.setXid(rs.getString(this.xidIndex));
        vo.setData(this.extractJson.extract(rs.getClob(jsonDataIndex)));

        vo.setDefinition(definition);
        vo.setSourceId(sourceId);

        //Read Into Detector
        JsonTypeReader typeReader = new JsonTypeReader(rs.getString(dataIndex));
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

        MangoPermission read = new MangoPermission(rs.getInt(readPermissionIndex));
        vo.supplyReadPermission(() -> read);
        MangoPermission edit = new MangoPermission(rs.getInt(editPermissionIndex));
        vo.supplyEditPermission(() -> edit);

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