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
public class MailingListTableDefinition extends AbstractTableDefinition {

    public static final String TABLE_NAME = "mailingLists";

    public MailingListTableDefinition() {
        super(DSL.table(TABLE_NAME), DSL.name("ml"));
    }
    
    @Override
    protected void addFields(List<Field<?>> fields) {
        super.addFields(fields);
        fields.add(DSL.field(DSL.name("receiveAlarmEmails"), SQLDataType.INTEGER.nullable(false)));
    }

}
