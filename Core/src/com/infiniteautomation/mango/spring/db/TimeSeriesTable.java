/*
 * Copyright (C) 2020 Infinite Automation Systems Inc. All rights reserved.
 */

package com.infiniteautomation.mango.spring.db;

import org.jooq.Identity;
import org.jooq.TableField;
import org.jooq.impl.CustomTable;
import org.jooq.impl.DSL;
import org.jooq.impl.Internal;
import org.jooq.impl.SQLDataType;

/**
 * @author Jared Wiltshire
 */
public class TimeSeriesTable extends CustomTable<TimeSeriesRecord> {
    private static final long serialVersionUID = 1L;

    public static final TimeSeriesTable TIME_SERIES = new TimeSeriesTable();
    public final TableField<TimeSeriesRecord, Integer> ID = createField(DSL.name("id"), SQLDataType.INTEGER.identity(true));

    protected TimeSeriesTable() {
        super(DSL.name("timeSeries"));
    }

    @Override
    public Class<? extends TimeSeriesRecord> getRecordType() {
        return TimeSeriesRecord.class;
    }

    @Override
    public Identity<TimeSeriesRecord, ?> getIdentity() {
        return Internal.createIdentity(this, ID);
    }
}
