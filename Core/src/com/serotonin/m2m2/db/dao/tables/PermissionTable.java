/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.db.dao.tables;

import org.jooq.TableField;
import org.jooq.impl.CustomTable;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

public class PermissionTable extends CustomTable<PermissionRecord> {
    private static final long serialVersionUID = 1L;

    public static final PermissionTable PERMISSIONS = new PermissionTable();

    public final TableField<PermissionRecord, Integer> id = createField(DSL.name("id"), SQLDataType.INTEGER.nullable(false));

    protected PermissionTable() {
        super(DSL.name("permissions"));
    }

    @Override
    public Class<? extends PermissionRecord> getRecordType() {
        return PermissionRecord.class;
    }
}