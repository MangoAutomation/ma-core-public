/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.db;

import java.util.List;

import org.jooq.Field;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.springframework.stereotype.Component;

/**
 * @author Terry Packer
 *
 */
@Component
public class AuditEventTableDefinition extends AbstractBasicTableDefinition {

    public static final String TABLE_NAME = "audit";
    
    public AuditEventTableDefinition() {
        super(DSL.table(TABLE_NAME), DSL.name("aud"));
    }

    @Override
    protected void addFields(List<Field<?>> fields) {
        fields.add(DSL.field(DSL.name("typeName"), SQLDataType.VARCHAR(32).nullable(false)));
        fields.add(DSL.field(DSL.name("alarmLevel"), SQLDataType.VARCHAR(32).nullable(false)));
        fields.add(DSL.field(DSL.name("userId"), SQLDataType.INTEGER.nullable(false)));
        fields.add(DSL.field(DSL.name("changeType"), SQLDataType.INTEGER.nullable(false)));
        fields.add(DSL.field(DSL.name("objectId"), SQLDataType.INTEGER.nullable(false)));
        fields.add(DSL.field(DSL.name("ts"), SQLDataType.BIGINT.nullable(false)));
        fields.add(DSL.field(DSL.name("context"), SQLDataType.CLOB.nullable(true)));
        fields.add(DSL.field(DSL.name("message"), SQLDataType.VARCHAR(255).nullable(true)));
    }
}
