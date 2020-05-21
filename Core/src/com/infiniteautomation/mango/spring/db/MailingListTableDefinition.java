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
public class MailingListTableDefinition extends AbstractTableDefinition {

    public static final String TABLE_NAME = "mailingLists";
    public static final Table<Record> TABLE = DSL.table(TABLE_NAME);
    public static final Field<Integer> ID = DSL.field(TABLE.getQualifiedName().append("id"), SQLDataType.INTEGER.nullable(false));
    public static final Field<Integer> READ_PERMISSION = DSL.field(TABLE.getQualifiedName().append("readPermissionId"), SQLDataType.INTEGER.nullable(false));
    public static final Field<Integer> EDIT_PERMISSION = DSL.field(TABLE.getQualifiedName().append("editPermissionId"), SQLDataType.INTEGER.nullable(false));

    public static final Field<Integer> READ_PERMISSION_ALIAS = DSL.field( DSL.name("ml").append("readPermissionId"), SQLDataType.INTEGER.nullable(false));
    public static final Field<Integer> EDIT_PERMISSION_ALIAS = DSL.field( DSL.name("ml").append("editPermissionId"), SQLDataType.INTEGER.nullable(false));

    public MailingListTableDefinition() {
        super(DSL.table(TABLE_NAME), DSL.name("ml"));
    }

    @Override
    protected void addFields(List<Field<?>> fields) {
        super.addFields(fields);
        fields.add(DSL.field(DSL.name("receiveAlarmEmails"), SQLDataType.INTEGER.nullable(false)));
        fields.add(DSL.field(DSL.name("readPermissionId"), SQLDataType.INTEGER.nullable(false)));
        fields.add(DSL.field(DSL.name("editPermissionId"), SQLDataType.INTEGER.nullable(false)));
    }

}
