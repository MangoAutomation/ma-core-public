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
public class PointValueAnnotationTable extends CustomTable<PointValueAnnotationRecord> {

    public static final PointValueAnnotationTable POINT_VALUE_ANNOTATIONS = new PointValueAnnotationTable();

    public final TableField<PointValueAnnotationRecord, Long> pointValueId = createField(DSL.name("pointValueId"), SQLDataType.BIGINT.identity(true));
    public final TableField<PointValueAnnotationRecord, String> textPointValueShort = createField(DSL.name("textPointValueShort"), SQLDataType.VARCHAR(128));
    public final TableField<PointValueAnnotationRecord, String> textPointValueLong = createField(DSL.name("textPointValueLong"), SQLDataType.CLOB);
    public final TableField<PointValueAnnotationRecord, String> sourceMessage = createField(DSL.name("sourceMessage"), SQLDataType.CLOB);

    protected PointValueAnnotationTable() {
        super(DSL.name("pointValueAnnotations"));
    }

    @Override
    public Class<? extends PointValueAnnotationRecord> getRecordType() {
        return PointValueAnnotationRecord.class;
    }

    @Override
    public Identity<PointValueAnnotationRecord, ?> getIdentity() {
        return Internal.createIdentity(this, pointValueId);
    }

}
