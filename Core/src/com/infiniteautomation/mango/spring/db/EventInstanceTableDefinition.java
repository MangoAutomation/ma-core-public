/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.db;

import java.util.List;

import org.jooq.Field;
import org.jooq.Name;
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
public class EventInstanceTableDefinition extends AbstractTableDefinition {

    public static final String TABLE_ALIAS = "evt";
    public static final String TABLE_NAME = "events";
    public static final Table<Record> TABLE = DSL.table(TABLE_NAME);

    public static final Field<Integer> READ_PERMISSION = DSL.field(TABLE.getQualifiedName().append("readPermissionId"), SQLDataType.INTEGER.nullable(false));
    public static final Field<Integer> READ_PERMISSION_ALIAS = DSL.field( DSL.name(TABLE_ALIAS).append("readPermissionId"), SQLDataType.INTEGER.nullable(false));

    public EventInstanceTableDefinition() {
        super(DSL.table(TABLE_NAME), DSL.name(TABLE_ALIAS));
    }

    @Override
    protected void addFields(List<Field<?>> fields) {
        super.addFields(fields);
        fields.add(DSL.field(DSL.name("typeName"), SQLDataType.VARCHAR(32).nullable(false)));
        fields.add(DSL.field(DSL.name("subtypeName"), SQLDataType.VARCHAR(32)));
        fields.add(DSL.field(DSL.name("typeRef1"), SQLDataType.INTEGER));
        fields.add(DSL.field(DSL.name("typeRef2"), SQLDataType.INTEGER));
        fields.add(DSL.field(DSL.name("activeTs"), SQLDataType.BIGINT));
        fields.add(DSL.field(DSL.name("rtnApplicable"), SQLDataType.CHAR(1)));
        fields.add(DSL.field(DSL.name("rtnTs"), SQLDataType.BIGINT));
        fields.add(DSL.field(DSL.name("rtnCause"), SQLDataType.INTEGER));
        fields.add(DSL.field(DSL.name("alarmLevel"), SQLDataType.INTEGER));
        fields.add(DSL.field(DSL.name("message"), SQLDataType.CLOB));
        fields.add(DSL.field(DSL.name("ackTs"), SQLDataType.BIGINT));
        fields.add(DSL.field(DSL.name("ackUserId"), SQLDataType.INTEGER));
        fields.add(DSL.field(DSL.name("alternateAckSource"), SQLDataType.CLOB));
        fields.add(READ_PERMISSION);
    }

    @Override
    protected Name getXidFieldName() {
        return null;
    }
    @Override
    protected Name getNameFieldName() {
        return null;
    }
}
