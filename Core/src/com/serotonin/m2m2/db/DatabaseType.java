/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db;

import com.serotonin.ShouldNeverHappenException;

public enum DatabaseType {
    @Deprecated
    DERBY {
        @Override
        AbstractDatabaseProxy getImpl() {
            throw new ShouldNeverHappenException("Derby database support removed, please convert your database to H2 or MySQL using a 2.x.x version of Mango.");
        }
    },
    H2 {
        @Override
        AbstractDatabaseProxy getImpl() {
            return new H2Proxy();
        }
    },
    MSSQL {
        @Override
        AbstractDatabaseProxy getImpl() {
            return new MSSQLProxy();
        }
    },
    MYSQL {
        @Override
        AbstractDatabaseProxy getImpl() {
            return new MySQLProxy();
        }
    },
    POSTGRES {
        @Override
        AbstractDatabaseProxy getImpl() {
            return new PostgresProxy();
        }
    };

    abstract AbstractDatabaseProxy getImpl();
}
