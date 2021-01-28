/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;

public class DefaultDatabaseProxyFactory implements DatabaseProxyFactory {

    @Override
    public AbstractDatabaseProxy createDatabaseProxy(DatabaseType type) {
        boolean useMetrics = Common.envProps.getBoolean("db.useMetrics", false);

        switch(type) {
            case H2:
                return new H2Proxy(this, useMetrics);
            case MSSQL:
                return new MSSQLProxy(this, useMetrics);
            case MYSQL:
                return new MySQLProxy(this, useMetrics);
            case POSTGRES:
                return new PostgresProxy(this, useMetrics);
            default:
                throw new ShouldNeverHappenException("Unknown/unsupported database type " + type);
        }
    }

}
