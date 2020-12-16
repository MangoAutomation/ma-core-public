/*
 * Copyright (C) 2020 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db.dao.tables;

import org.jooq.impl.CustomRecord;

/**
 * @author Jared Wiltshire
 */
public class PointValueAnnotationRecord extends CustomRecord<PointValueAnnotationRecord> {
    protected PointValueAnnotationRecord() {
        super(PointValueAnnotationTable.POINT_VALUE_ANNOTATIONS);
    }
}