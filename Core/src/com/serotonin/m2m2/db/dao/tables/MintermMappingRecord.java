/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.db.dao.tables;

import org.jooq.impl.CustomRecord;

/**
 * @author Jared Wiltshire
 */
public class MintermMappingRecord extends CustomRecord<MintermMappingRecord> {
    private static final long serialVersionUID = 1L;

    protected MintermMappingRecord() {
        super(MintermMappingTable.MINTERMS_MAPPING);
    }

}