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
 *
 * @author Terry Packer
 */
@Component
public class MangoSessionDataTableDefinition extends AbstractBasicTableDefinition {

    public static final String TABLE_NAME = "mangoSessionData";

    public MangoSessionDataTableDefinition() {
        super(DSL.table(TABLE_NAME), DSL.name("msd"));
    }

    @Override
    public Field<Integer> getIdField() {
        return null;
    }

    @Override
    public Field<Integer> getIdAlias() {
        return null;
    }

    @Override
    protected void addFields(List<Field<?>> fields) {
        fields.add(DSL.field(DSL.name("sessionId"), SQLDataType.VARCHAR(120)));
        fields.add(DSL.field(DSL.name("contextPath"), SQLDataType.VARCHAR(60)));
        fields.add(DSL.field(DSL.name("virtualHost"), SQLDataType.VARCHAR(60)));
        fields.add(DSL.field(DSL.name("lastNode"), SQLDataType.VARCHAR(60)));
        fields.add(DSL.field(DSL.name("accessTime"), SQLDataType.BIGINT));
        fields.add(DSL.field(DSL.name("lastAccessTime"), SQLDataType.BIGINT));
        fields.add(DSL.field(DSL.name("createTime"), SQLDataType.BIGINT));
        fields.add(DSL.field(DSL.name("cookieTime"), SQLDataType.BIGINT));
        fields.add(DSL.field(DSL.name("lastSavedTime"), SQLDataType.BIGINT));
        fields.add(DSL.field(DSL.name("expiryTime"), SQLDataType.BIGINT));
        fields.add(DSL.field(DSL.name("maxInterval"), SQLDataType.BIGINT));
        fields.add(DSL.field(DSL.name("userId"), SQLDataType.INTEGER));
    }

}
