/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.db.dao.tables;

import org.jooq.TableField;
import org.jooq.impl.CustomTable;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

public class MintermMappingTable extends CustomTable<MintermMappingRecord> {
    private static final long serialVersionUID = 1L;

    public static final MintermMappingTable MINTERMS_MAPPING = new MintermMappingTable();

    public final TableField<MintermMappingRecord, Integer> mintermId = createField(DSL.name("minterm_id"), SQLDataType.INTEGER.nullable(false));
    public final TableField<MintermMappingRecord, Integer> roleId = createField(DSL.name("role_id"), SQLDataType.INTEGER.nullable(false));

    protected MintermMappingTable() {
        super(DSL.name("minterms_roles"));
    }

    @Override
    public Class<? extends MintermMappingRecord> getRecordType() {
        return MintermMappingRecord.class;
    }

}