/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.db;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.jooq.Field;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

/**
 * @author Terry Packer
 *
 */
public abstract class AbstractBasicTableDefinition {

    protected final Table<? extends Record> table;
    protected final Name alias;
    protected final Table<? extends Record> tableAsAlias;
    
    protected final LinkedHashMap<String, Field<?>> fieldMap;
    protected final LinkedHashMap<String, Field<?>> aliasMap;
    
    protected final Field<Integer> idField;
    protected final Field<Integer> idAlias;
    
    protected final List<Field<?>> insertFields;
    protected final List<Field<?>> updateFields;
    protected final List<Field<?>> selectFields;
    
    public AbstractBasicTableDefinition(Table<? extends Record> table, Name alias) {
        this.table = table;
        this.alias = alias;
        this.tableAsAlias = this.table.as(this.alias);
        this.idField = getIdField();
        this.idAlias = getIdAlias();
        
        this.insertFields = new ArrayList<>();
        this.updateFields = new ArrayList<>();
        this.selectFields = new ArrayList<>();
        
        this.fieldMap = new LinkedHashMap<>();
        this.aliasMap = new LinkedHashMap<>();
    }

    @PostConstruct
    public void initFields() {
        if(idField != null) {
            this.fieldMap.put(idField.getName(), idField);
            this.selectFields.add(DSL.field(this.alias.append(idField.getName()), idField.getDataType()));
        }
        
        List<Field<?>> fields = new ArrayList<>();
        addFields(fields);
        
        //Fill in the fields for the queries
        for (Field<?> field : fields) {
            this.selectFields.add(DSL.field(this.alias.append(field.getName()), field.getDataType()));
            this.insertFields.add(field);
            this.updateFields.add(field);
            this.fieldMap.put(field.getName(), field);
        }
        addFieldMappings(this.fieldMap);
        
        //Generate the aliases
        for(Field<?> field : this.fieldMap.values()) {
            this.aliasMap.put(field.getName(), DSL.field(this.alias.append(field.getName()), field.getDataType()));
        }
        
    }
    
    /**
     * populate a list of fields in order that they should appear in the
     * they query 
     * @return
     */
    abstract protected void addFields(List<Field<?>> fields);
    
    /**
     * Add in mappings for alternate names to be used in queries for existing fields.
     *  i.e.  query for dataTypeId based on the alias of 'dataType'
     * @param map
     */
    protected void addFieldMappings(Map<String, Field<?>> map) {
        //No-op by default
    }

    /**
     * Override as necessary Can be null if no Pk Exists
     *
     * @return String name of Pk Column
     */
    public Field<Integer> getIdField() {
        return DSL.field(DSL.name(getIdFieldName()), SQLDataType.INTEGER.nullable(false));
    }
    
    public Field<Integer> getIdAlias() {
        return DSL.field(this.alias.append(getIdFieldName()), SQLDataType.INTEGER.nullable(false));
    }
    
    /**
     * Optionally override the name of the id column
     * @return
     */
    protected Name getIdFieldName() {
        return DSL.name("id");
    }
    
    public Table<? extends Record> getTable() {
        return table; 
    }
    
    /**
     * Return the map of fields that can be iterated in insertion order
     *  for use in queries etc these ARE NOT prepended with the table alias
     * @return
     */
    public LinkedHashMap<String, Field<?>> getFieldMap() {
        return fieldMap;
    }
    
    /**
     * Return the map of field aliases that can be iterated in insertion order
     *  for use in queries etc these ARE prepended with the table alias
     * @return
     */
    public LinkedHashMap<String, Field<?>> getAliasMap() {
        return aliasMap;
    }
    
    /**
     * Get the Field for the column
     * @param columnName
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> Field<T> getField(String columnName) {
        return (Field<T>) fieldMap.get(columnName);
    }
    
    /**
     * Get the field prepended with the table alias
     * @param columnName
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> Field<T> getAlias(String columnName) {
        return (Field<T>) aliasMap.get(columnName);
    }

    public Name getAlias() {
        return alias;
    }

    public Table<? extends Record> getTableAsAlias() {
        return tableAsAlias;
    }

    public List<Field<?>> getInsertFields() {
        return insertFields;
    }

    public List<Field<?>> getUpdateFields() {
        return updateFields;
    }

    public List<Field<?>> getSelectFields() {
        return selectFields;
    }
}
