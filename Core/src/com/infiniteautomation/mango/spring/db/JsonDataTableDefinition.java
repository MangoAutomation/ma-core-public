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
public class JsonDataTableDefinition extends AbstractTableDefinition {
    
    public static final String TABLE_NAME = "jsonData";
    
    public JsonDataTableDefinition() {
        super(DSL.table(TABLE_NAME), DSL.name("jd"));
    }
    
    @Override
    protected void addFields(List<Field<?>> fields) {
        super.addFields(fields);
        fields.add(DSL.field(DSL.name("publicData"), SQLDataType.CHAR(1)));
        fields.add(DSL.field(DSL.name("data"), SQLDataType.CLOB));
    }

}
