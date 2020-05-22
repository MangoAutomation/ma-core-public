/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.db.dao.tables;

import org.jooq.TableField;
import org.jooq.impl.CustomTable;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

public class PermissionMappingTable extends CustomTable<PermissionMappingRecord> {
    private static final long serialVersionUID = 1L;

    public static final PermissionMappingTable PERMISSIONS_MAPPING = new PermissionMappingTable();

    public final TableField<PermissionMappingRecord, Integer> permissionId = createField(DSL.name("permissionId"), SQLDataType.INTEGER.nullable(false));
    public final TableField<PermissionMappingRecord, Integer> mintermId = createField(DSL.name("mintermId"), SQLDataType.INTEGER.nullable(false));

    protected PermissionMappingTable() {
        super(DSL.name("permissionsMinterms"));
    }

    @Override
    public Class<? extends PermissionMappingRecord> getRecordType() {
        return PermissionMappingRecord.class;
    }
}