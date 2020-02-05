/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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

    /**
     * Insertion ordered
     */
    protected Map<String, Field<?>> fieldMap;
    /**
     * Insertion ordered
     */
    protected Map<String, Field<?>> aliasMap;

    protected final Field<Integer> idField;
    protected final Field<Integer> idAlias;

    protected List<Field<?>> insertFields;
    protected List<Field<?>> updateFields;
    protected List<Field<?>> selectFields;

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

        //Generate the aliases
        for(Entry<String, Field<?>> entry : this.fieldMap.entrySet()) {
            this.aliasMap.put(entry.getKey(), DSL.field(this.alias.append(entry.getValue().getName()), entry.getValue().getDataType()));
        }

        //Now add the mappings
        Map<String, Field<?>> mappings = getAliasMappings();
        if(mappings != null) {
            this.aliasMap.putAll(mappings);
        }

        //Make all unmodifiable
        this.selectFields = Collections.unmodifiableList(this.selectFields);
        this.insertFields = Collections.unmodifiableList(this.insertFields);
        this.updateFields = Collections.unmodifiableList(this.updateFields);

        this.fieldMap = Collections.unmodifiableMap(this.fieldMap);
        this.aliasMap = Collections.unmodifiableMap(this.aliasMap);
    }

    /**
     * populate a list of fields in order that they should appear in the
     * they query
     * @return
     */
    abstract protected void addFields(List<Field<?>> fields);

    /**
     * Add in mappings for alternate names to be used in queries.  These should be in alias form with the correct table prefix.
     * They will only be available in the alias map for this table.
     * NOTE: The alias and field maps are usable in this method for example if
     *  one wanted to create a mapping for an existing alias or field.
     *
     *  i.e.  query for dataTypeId based on the alias of 'ds.dataType'
     *  i.e.  query for username based on the alias of 'u.username'
     * @return
     */
    protected Map<String, Field<?>> getAliasMappings() {
        return null;
    }

    /**
     * Override as necessary Can be null if no Pk Exists
     *
     * @return String name of Pk Column
     */
    public Field<Integer> getIdField() {
        return DSL.field(DSL.name(getIdFieldName()), SQLDataType.INTEGER.nullable(false).identity(true));
    }

    public Field<Integer> getIdAlias() {
        return DSL.field(this.alias.append(getIdFieldName()), SQLDataType.INTEGER.nullable(false).identity(true));
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
    public Map<String, Field<?>> getFieldMap() {
        return fieldMap;
    }

    /**
     * Return the map of field aliases that can be iterated in insertion order
     *  for use in queries etc these ARE prepended with the table alias
     * @return
     */
    public Map<String, Field<?>> getAliasMap() {
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
