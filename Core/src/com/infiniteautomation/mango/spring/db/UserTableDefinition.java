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
public class UserTableDefinition extends AbstractTableDefinition {

    public static final String TABLE_NAME = "users";
    
    /**
     * @param table
     * @param alias
     */
    public UserTableDefinition() {
        super(DSL.table(TABLE_NAME), DSL.name("u"));
    }
    
    @Override
    protected void addFields(List<Field<?>> fields) {
        super.addFields(fields);
        fields.add(DSL.field(DSL.name("password"), SQLDataType.VARCHAR(255).nullable(false)));
        fields.add(DSL.field(DSL.name("email"), SQLDataType.VARCHAR(255).nullable(false)));
        fields.add(DSL.field(DSL.name("phone"), SQLDataType.VARCHAR(40)));
        fields.add(DSL.field(DSL.name("disabled"), SQLDataType.CHAR(1)));
        fields.add(DSL.field(DSL.name("lastLogin"), SQLDataType.BIGINT));
        fields.add(DSL.field(DSL.name("homeUrl"), SQLDataType.VARCHAR(255)));
        fields.add(DSL.field(DSL.name("receiveAlarmEmails"), SQLDataType.INTEGER.nullable(false)));
        fields.add(DSL.field(DSL.name("receiveOwnAuditEvents"), SQLDataType.CHAR(1)));
        fields.add(DSL.field(DSL.name("timezone"), SQLDataType.VARCHAR(50)));
        fields.add(DSL.field(DSL.name("muted"), SQLDataType.CHAR(1)));
        fields.add(DSL.field(DSL.name("locale"), SQLDataType.VARCHAR(50)));
        fields.add(DSL.field(DSL.name("tokenVersion"), SQLDataType.INTEGER.nullable(false)));
        fields.add(DSL.field(DSL.name("passwordVersion"), SQLDataType.INTEGER.nullable(false)));
        fields.add(DSL.field(DSL.name("passwordChangeTimestamp"), SQLDataType.BIGINT.nullable(false)));
        fields.add(DSL.field(DSL.name("sessionExpirationOverride"), SQLDataType.CHAR(1)));
        fields.add(DSL.field(DSL.name("sessionExpirationPeriods"), SQLDataType.INTEGER));
        fields.add(DSL.field(DSL.name("sessionExpirationPeriodType"), SQLDataType.VARCHAR(25)));
        fields.add(DSL.field(DSL.name("organization"), SQLDataType.VARCHAR(80)));
        fields.add(DSL.field(DSL.name("organizationalRole"), SQLDataType.VARCHAR(80)));
        fields.add(DSL.field(DSL.name("createdTs"), SQLDataType.BIGINT.nullable(false)));
        fields.add(DSL.field(DSL.name("emailVerifiedTs"), SQLDataType.BIGINT));
        fields.add(DSL.field(DSL.name("data"), SQLDataType.CLOB));
    }
    
    @Override
    protected Name getXidFieldName() {
        return DSL.name("username");
    }
    
    @Override
    protected int getXidFieldLength() {
        return 40;
    }

}
