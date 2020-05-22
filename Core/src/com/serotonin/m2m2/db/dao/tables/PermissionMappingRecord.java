/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.db.dao.tables;

import org.jooq.impl.CustomRecord;

/**
 * @author Jared Wiltshire
 */
public class PermissionMappingRecord extends CustomRecord<PermissionMappingRecord> {
    private static final long serialVersionUID = 1L;

    protected PermissionMappingRecord() {
        super(PermissionMappingTable.PERMISSIONS_MAPPING);
    }
}