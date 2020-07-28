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
public class EventHandlerTableDefinition extends AbstractTableDefinition {

    public static final Table<? extends Record> EVENT_HANDLER_MAPPING_TABLE = DSL.table("eventHandlersMapping");
    public static final Name EVENT_HANDLER_MAPPING_ALIAS = DSL.name("ehm");

    public static final Field<String> EVENT_HANDLER_MAPPING_EVENT_TYPE_NAME = DSL.field(DSL.name("eventTypeName"), SQLDataType.VARCHAR(32).nullable(false));
    public static final Field<String> EVENT_HANDLER_MAPPING_EVENT_TYPE_NAME_ALIAS = DSL.field(EVENT_HANDLER_MAPPING_ALIAS.append(DSL.name("eventTypeName")), SQLDataType.VARCHAR(32).nullable(false));

    public static final Field<String> EVENT_HANDLER_MAPPING_EVENT_SUB_TYPE_NAME = DSL.field(DSL.name("eventSubtypeName"), SQLDataType.VARCHAR(32).nullable(false));
    public static final Field<String> EVENT_HANDLER_MAPPING_EVENT_SUB_TYPE_NAME_ALIAS = DSL.field(EVENT_HANDLER_MAPPING_ALIAS.append(DSL.name("eventSubtypeName")), SQLDataType.VARCHAR(32).nullable(false));

    public static final Field<Integer> EVENT_HANDLER_MAPPING_TYPEREF1 = DSL.field(DSL.name("eventTypeRef1"), SQLDataType.INTEGER.nullable(false));
    public static final Field<Integer> EVENT_HANDLER_MAPPING_TYPEREF1_ALIAS = DSL.field(EVENT_HANDLER_MAPPING_ALIAS.append(DSL.name("eventTypeRef1")), SQLDataType.INTEGER.nullable(false));

    public static final Field<Integer> EVENT_HANDLER_MAPPING_TYPEREF2 = DSL.field(DSL.name("eventTypeRef2"), SQLDataType.INTEGER.nullable(false));
    public static final Field<Integer> EVENT_HANDLER_MAPPING_TYPEREF2_ALIAS = DSL.field(EVENT_HANDLER_MAPPING_ALIAS.append(DSL.name("eventTypeRef2")), SQLDataType.INTEGER.nullable(false));

    public static final String TABLE_NAME = "eventHandlers";
    public static final Table<Record> TABLE = DSL.table(TABLE_NAME);

    public static final String READ_PERMISSION_NAME = "readPermissionId";
    public static final String EDIT_PERMISSION_NAME = "editPermissionId";

    public static final Field<Integer> READ_PERMISSION = DSL.field(TABLE.getQualifiedName().append(READ_PERMISSION_NAME), SQLDataType.INTEGER.nullable(false));
    public static final Field<Integer> EDIT_PERMISSION = DSL.field(TABLE.getQualifiedName().append(EDIT_PERMISSION_NAME), SQLDataType.INTEGER.nullable(false));

    public static final Field<Integer> READ_PERMISSION_ALIAS = DSL.field( DSL.name("eh").append(READ_PERMISSION_NAME), SQLDataType.INTEGER.nullable(false));
    public static final Field<Integer> EDIT_PERMISSION_ALIAS = DSL.field( DSL.name("eh").append(EDIT_PERMISSION_NAME), SQLDataType.INTEGER.nullable(false));

    public EventHandlerTableDefinition() {
        super(TABLE, DSL.name("eh"));
    }

    @Override
    protected Name getNameFieldName() {
        return DSL.name("alias");
    }

    @Override
    protected void addFields(List<Field<?>> fields) {
        super.addFields(fields);
        fields.add(DSL.field(DSL.name("eventHandlerType"), SQLDataType.VARCHAR(40)));
        fields.add(DSL.field(DSL.name("data"), SQLDataType.BLOB));
        fields.add(DSL.field(DSL.name(READ_PERMISSION_NAME), SQLDataType.INTEGER.nullable(false)));
        fields.add(DSL.field(DSL.name(EDIT_PERMISSION_NAME), SQLDataType.INTEGER.nullable(false)));
    }

}
