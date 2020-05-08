/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.db.dao.tables;

import org.jooq.TableField;
import org.jooq.impl.CustomTable;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

public class MintermTable extends CustomTable<MintermRecord> {
    private static final long serialVersionUID = 1L;

    public static final MintermTable MINTERMS = new MintermTable();

    public final TableField<MintermRecord, Integer> id = createField(DSL.name("id"), SQLDataType.INTEGER.nullable(false));

    protected MintermTable() {
        super(DSL.name("minterms"));
    }

    @Override
    public Class<? extends MintermRecord> getRecordType() {
        return MintermRecord.class;
    }

}