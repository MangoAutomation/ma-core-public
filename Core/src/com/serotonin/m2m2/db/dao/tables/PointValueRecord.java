/*
 * Copyright (C) 2020 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db.dao.tables;

import org.jooq.impl.CustomRecord;

/**
 * @author Jared Wiltshire
 */
public class PointValueRecord extends CustomRecord<PointValueRecord> {
    protected PointValueRecord() {
        super(PointValueTable.POINT_VALUES);
    }
}