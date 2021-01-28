/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db;

import org.jooq.SQLDialect;

public enum DatabaseType {
    @Deprecated
    DERBY(SQLDialect.DERBY),
    H2(SQLDialect.H2),
    MSSQL(SQLDialect.DEFAULT),
    MYSQL(SQLDialect.MYSQL),
    POSTGRES(SQLDialect.POSTGRES);

    private final SQLDialect dialect;

    DatabaseType(SQLDialect dialect) {
        this.dialect = dialect;
    }

    public SQLDialect getDialect() {
        return dialect;
    }
}
