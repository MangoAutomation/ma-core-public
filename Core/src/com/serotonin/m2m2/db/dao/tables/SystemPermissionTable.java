/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.serotonin.m2m2.db.dao.tables;

import org.jooq.TableField;
import org.jooq.impl.CustomTable;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

/**
 *
 * @author Terry Packer
 */
public class SystemPermissionTable extends CustomTable<SystemPermissionRecord> {
    private static final long serialVersionUID = 1L;

    public static final SystemPermissionTable SYSTEM_PERMISSIONS = new SystemPermissionTable();

    public final TableField<SystemPermissionRecord, String> permissionType = createField(DSL.name("permissionType"), SQLDataType.VARCHAR(255));
    public final TableField<SystemPermissionRecord, Integer> permissionId = createField(DSL.name("permissionId"), SQLDataType.INTEGER.nullable(false));

    protected SystemPermissionTable() {
        super(DSL.name("systemPermissions"));
    }

    @Override
    public Class<? extends SystemPermissionRecord> getRecordType() {
        return SystemPermissionRecord.class;
    }
}
