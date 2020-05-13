/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.db;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.springframework.stereotype.Component;

/**
 * @author Terry Packer
 *
 */
@Component
public class DataPointTableDefinition extends AbstractTableDefinition {

    public static final String TABLE_NAME = "dataPoints";
    public static final Table<Record> TABLE = DSL.table(TABLE_NAME);
    public static final Field<Integer> ID = DSL.field(TABLE.getQualifiedName().append("id"), SQLDataType.INTEGER.nullable(false));
    public static final Field<Integer> READ_PERMISSION = DSL.field(TABLE.getQualifiedName().append("readPermissionId"), SQLDataType.INTEGER.nullable(true));
    public static final Field<Integer> SET_PERMISSION = DSL.field(TABLE.getQualifiedName().append("setPermissionId"), SQLDataType.INTEGER.nullable(true));

    public static final Field<Integer> READ_PERMISSION_ALIAS = DSL.field( DSL.name("dp").append("readPermissionId"), SQLDataType.INTEGER.nullable(true));
    public static final Field<Integer> SET_PERMISSION_ALIAS = DSL.field( DSL.name("dp").append("setPermissionId"), SQLDataType.INTEGER.nullable(true));

    public DataPointTableDefinition() {
        super(DSL.table(TABLE_NAME), DSL.name("dp"));
    }

    @Override
    protected void addFields(List<Field<?>> fields) {
        super.addFields(fields);
        fields.add(DSL.field(DSL.name("data"), SQLDataType.BLOB));
        fields.add(DSL.field(DSL.name("dataSourceId"), SQLDataType.INTEGER));
        fields.add(DSL.field(DSL.name("deviceName"), SQLDataType.VARCHAR(255).nullable(true)));
        fields.add(DSL.field(DSL.name("enabled"), SQLDataType.CHAR(1).nullable(true)));
        fields.add(DSL.field(DSL.name("loggingType"), SQLDataType.INTEGER));
        fields.add(DSL.field(DSL.name("intervalLoggingPeriodType"), SQLDataType.INTEGER));
        fields.add(DSL.field(DSL.name("intervalLoggingPeriod"), SQLDataType.INTEGER));
        fields.add(DSL.field(DSL.name("intervalLoggingType"), SQLDataType.INTEGER));
        fields.add(DSL.field(DSL.name("tolerance"), SQLDataType.DOUBLE));
        fields.add(DSL.field(DSL.name("purgeOverride"), SQLDataType.CHAR(1)));
        fields.add(DSL.field(DSL.name("purgeType"), SQLDataType.INTEGER));
        fields.add(DSL.field(DSL.name("purgePeriod"), SQLDataType.INTEGER));
        fields.add(DSL.field(DSL.name("defaultCacheSize"), SQLDataType.INTEGER));
        fields.add(DSL.field(DSL.name("discardExtremeValues"), SQLDataType.CHAR(1)));
        fields.add(DSL.field(DSL.name("engineeringUnits"), SQLDataType.INTEGER));
        fields.add(DSL.field(DSL.name("rollup"), SQLDataType.INTEGER));
        fields.add(DSL.field(DSL.name("dataTypeId"), SQLDataType.INTEGER));
        fields.add(DSL.field(DSL.name("settable"), SQLDataType.CHAR(1)));
        fields.add(DSL.field(DSL.name("jsonData"), SQLDataType.CLOB));
        fields.add(DSL.field(DSL.name("readPermissionId"), SQLDataType.INTEGER.nullable(true)));
        fields.add(DSL.field(DSL.name("setPermissionId"), SQLDataType.INTEGER.nullable(true)));
    }

    @Override
    protected Map<String, Field<?>> getAliasMappings() {
        Map<String, Field<?>> map = new HashMap<>();
        map.put("dataType", aliasMap.get("dataTypeId"));
        return map;
    }
}
