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
public class UserCommentTableDefinition extends AbstractTableDefinition {

    public static final String TABLE_NAME = "userComments";

    public UserCommentTableDefinition() {
        super(DSL.table(TABLE_NAME), DSL.name("uc"));
    }

    @Override
    protected void addFields(List<Field<?>> fields) {
        super.addFields(fields);
        fields.add(DSL.field(DSL.name("userId"), SQLDataType.INTEGER));
        fields.add(DSL.field(DSL.name("ts"), SQLDataType.BIGINT.nullable(false)));
        fields.add(DSL.field(DSL.name("commentText"), SQLDataType.VARCHAR(1024).nullable(false)));
        fields.add(DSL.field(DSL.name("commentType"), SQLDataType.INTEGER.nullable(false)));
        fields.add(DSL.field(DSL.name("typeKey"), SQLDataType.INTEGER.nullable(false)));
    }

    @Override
    protected Name getNameFieldName() {
        return null;
    }
}
