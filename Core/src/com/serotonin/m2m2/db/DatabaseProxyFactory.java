/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db;

@FunctionalInterface
public interface DatabaseProxyFactory {
    DatabaseProxy createDatabaseProxy(DatabaseType type);

    DatabaseProxyFactory UNSUPPORTED_INSTANCE = t -> {
        throw new UnsupportedOperationException();
    };
}
