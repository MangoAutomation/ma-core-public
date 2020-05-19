/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.db;

import java.util.List;

import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.springframework.stereotype.Component;

/**
 * @author Terry Packer
 *
 */
@Component
public class DataSourceTableDefinition extends AbstractTableDefinition {

    public static final String TABLE_NAME = "dataSources";
    public static final Table<Record> TABLE = DSL.table(TABLE_NAME);
    public static final Field<Integer> ID = DSL.field(TABLE.getQualifiedName().append("id"), SQLDataType.INTEGER.nullable(false));
    public static final Field<Integer> READ_PERMISSION = DSL.field(TABLE.getQualifiedName().append("readPermissionId"), SQLDataType.INTEGER.nullable(true));
    public static final Field<Integer> EDIT_PERMISSION = DSL.field(TABLE.getQualifiedName().append("editPermissionId"), SQLDataType.INTEGER.nullable(true));

    public static final Field<Integer> READ_PERMISSION_ALIAS = DSL.field( DSL.name("ds").append("readPermissionId"), SQLDataType.INTEGER.nullable(true));
    public static final Field<Integer> EDIT_PERMISSION_ALIAS = DSL.field( DSL.name("ds").append("editPermissionId"), SQLDataType.INTEGER.nullable(true));

    public DataSourceTableDefinition() {
        super(DSL.table(TABLE_NAME), DSL.name("ds"));
    }

    @Override
    protected void addFields(List<Field<?>> fields) {
        super.addFields(fields);
        fields.add(DSL.field(DSL.name("dataSourceType"), SQLDataType.VARCHAR(40)));
        fields.add(DSL.field(DSL.name("data"), SQLDataType.BLOB));
        fields.add(DSL.field(DSL.name("jsonData"), SQLDataType.CLOB));
        fields.add(DSL.field(DSL.name("readPermissionId"), SQLDataType.INTEGER.nullable(true)));
        fields.add(DSL.field(DSL.name("editPermissionId"), SQLDataType.INTEGER.nullable(true)));
    }

}
