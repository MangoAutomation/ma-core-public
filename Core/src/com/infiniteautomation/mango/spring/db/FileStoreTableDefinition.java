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
public class FileStoreTableDefinition extends AbstractTableDefinition {
    public static final String TABLE_NAME = "fileStores";

    public static final Field<Integer> READ_PERMISSION_ALIAS = DSL.field( DSL.name("fs").append("readPermissionId"), SQLDataType.INTEGER.nullable(false));
    public static final Field<Integer> WRITE_PERMISSION_ALIAS = DSL.field( DSL.name("fs").append("writePermissionId"), SQLDataType.INTEGER.nullable(false));


    public FileStoreTableDefinition() {
        super(DSL.table(TABLE_NAME), DSL.name("fs"));
    }

    @Override
    protected void addFields(List<Field<?>> fields) {
        super.addFields(fields);
        fields.add(DSL.field(DSL.name("readPermissionId"), SQLDataType.INTEGER.nullable(true)));
        fields.add(DSL.field(DSL.name("writePermissionId"), SQLDataType.INTEGER.nullable(true)));
    }

}
