/*
 * Copyright (C) 2020 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao.tables;

import org.jooq.Identity;
import org.jooq.TableField;
import org.jooq.impl.CustomTable;
import org.jooq.impl.DSL;
import org.jooq.impl.Internal;
import org.jooq.impl.SQLDataType;

/**
 * @author Jared Wiltshire
 */
public class PointValueTable extends CustomTable<PointValueRecord> {

    public static final PointValueTable POINT_VALUES = new PointValueTable();

    public final TableField<PointValueRecord, Long> id = createField(DSL.name("id"), SQLDataType.BIGINT.identity(true));
    public final TableField<PointValueRecord, Integer> dataPointId = createField(DSL.name("dataPointId"), SQLDataType.INTEGER.nullable(false));
    public final TableField<PointValueRecord, Integer> dataType = createField(DSL.name("dataType"), SQLDataType.INTEGER.nullable(false));
    public final TableField<PointValueRecord, Double> pointValue = createField(DSL.name("pointValue"), SQLDataType.DOUBLE);
    public final TableField<PointValueRecord, Long> ts = createField(DSL.name("ts"), SQLDataType.BIGINT.nullable(false));

    protected PointValueTable() {
        super(DSL.name("pointValues"));
    }

    @Override
    public Class<? extends PointValueRecord> getRecordType() {
        return PointValueRecord.class;
    }

    @Override
    public Identity<PointValueRecord, ?> getIdentity() {
        return Internal.createIdentity(this, id);
    }
}
