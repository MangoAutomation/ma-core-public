/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.db;

import java.util.List;

import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.springframework.stereotype.Component;

/**
 * @author Terry Packer
 *
 */
@Component
public class EventHandlerTableDefinition extends AbstractTableDefinition {
    
    public static final String TABLE_NAME = "eventHandlers";
    
    public EventHandlerTableDefinition() {
        super(DSL.table(TABLE_NAME), DSL.name("eh"));
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
    }
    
}
