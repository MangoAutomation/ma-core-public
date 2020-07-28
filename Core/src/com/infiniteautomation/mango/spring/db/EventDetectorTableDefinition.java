/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.db;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jooq.Field;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.springframework.stereotype.Component;

import com.serotonin.m2m2.module.EventDetectorDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;

/**
 * @author Terry Packer
 *
 */
@Component
public class EventDetectorTableDefinition extends AbstractTableDefinition {

    public static final String TABLE_NAME = "eventDetectors";
    public static final Table<Record> TABLE = DSL.table(TABLE_NAME);
    public static final Field<Integer> ID = DSL.field(TABLE.getQualifiedName().append("id"), SQLDataType.INTEGER.nullable(false));
    public static final Field<Integer> READ_PERMISSION = DSL.field(TABLE.getQualifiedName().append("readPermissionId"), SQLDataType.INTEGER.nullable(false));
    public static final Field<Integer> EDIT_PERMISSION = DSL.field(TABLE.getQualifiedName().append("editPermissionId"), SQLDataType.INTEGER.nullable(false));

    public static final Field<Integer> READ_PERMISSION_ALIAS = DSL.field( DSL.name("edt").append("readPermissionId"), SQLDataType.INTEGER.nullable(false));
    public static final Field<Integer> EDIT_PERMISSION_ALIAS = DSL.field( DSL.name("edt").append("editPermissionId"), SQLDataType.INTEGER.nullable(false));

    public static final String SOURCE_TYPE_NAME_NAME = "sourceTypeName";
    public static final String TYPE_NAME_NAME = "typeName";
    public static final String JSON_DATA_NAME = "jsonData";
    public static final String DATA_NAME = "data";
    public static final String READ_PERMISSION_NAME = "readPermissionId";
    public static final String EDIT_PERMISSION_NAME = "editPermissionId";

    public EventDetectorTableDefinition() {
        super(DSL.table(TABLE_NAME), DSL.name("edt"));
    }

    @Override
    protected Name getNameFieldName() {
        return null;
    }

    @Override
    protected void addFields(List<Field<?>> fields) {
        super.addFields(fields);
        fields.add(DSL.field(DSL.name(SOURCE_TYPE_NAME_NAME), SQLDataType.VARCHAR(32)));
        fields.add(DSL.field(DSL.name(TYPE_NAME_NAME), SQLDataType.VARCHAR(32)));
        fields.add(DSL.field(DSL.name(JSON_DATA_NAME), SQLDataType.CLOB));
        fields.add(DSL.field(DSL.name(DATA_NAME), SQLDataType.CLOB));
        fields.add(DSL.field(DSL.name(READ_PERMISSION_NAME), SQLDataType.INTEGER.nullable(false)));
        fields.add(DSL.field(DSL.name(EDIT_PERMISSION_NAME), SQLDataType.INTEGER.nullable(false)));

        Map<String, Field<?>> definitionFields = new LinkedHashMap<>();
        //Build our ordered column set from the Module Registry
        List<EventDetectorDefinition<?>> defs = ModuleRegistry.getEventDetectorDefinitions();
        for(EventDetectorDefinition<?> def : defs) {
            definitionFields.put(def.getSourceIdColumnName(), DSL.field(DSL.name(def.getSourceIdColumnName()), SQLDataType.INTEGER));
        }
        fields.addAll(definitionFields.values());
    }

}
