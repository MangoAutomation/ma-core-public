/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.db.dao.tables;

import org.jooq.impl.CustomRecord;

/**
 * @author Terry Packer
 */
public class SystemPermissionRecord extends CustomRecord<SystemPermissionRecord> {
    private static final long serialVersionUID = 1L;

    protected SystemPermissionRecord() {
        super(SystemPermissionTable.SYSTEM_PERMISSIONS);
    }
}