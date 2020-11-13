/*
 * Copyright (C) 2020 Infinite Automation Systems Inc. All rights reserved.
 */

package com.infiniteautomation.mango.spring.db;

import org.jooq.impl.CustomRecord;

/**
 * @author Jared Wiltshire
 */
public class TimeSeriesRecord extends CustomRecord<TimeSeriesRecord> {
    private static final long serialVersionUID = 1L;

    protected TimeSeriesRecord() {
        super(TimeSeriesTable.TIME_SERIES);
    }
}
