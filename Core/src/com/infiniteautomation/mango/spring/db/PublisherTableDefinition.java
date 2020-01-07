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
public class PublisherTableDefinition extends AbstractTableDefinition {

    public static final String TABLE_NAME = "publishers";
    
    public PublisherTableDefinition() {
        super(DSL.table(TABLE_NAME), DSL.name("pub"));
    }
    
    @Override
    protected void addFields(List<Field<?>> fields) {
        super.addFields(fields);
        fields.add(DSL.field(DSL.name("publisherType"), SQLDataType.VARCHAR(40).nullable(false)));
        fields.add(DSL.field(DSL.name("data"), SQLDataType.BLOB));
    }

    @Override
    protected Name getNameFieldName() {
        return null;
    }

}
