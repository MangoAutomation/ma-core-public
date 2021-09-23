/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring.components;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.db.AbstractDatabaseProxy;
import com.serotonin.m2m2.db.DatabaseProxyFactory;
import com.serotonin.m2m2.db.DatabaseType;
import com.serotonin.m2m2.db.H2Proxy;
import com.serotonin.m2m2.db.MSSQLProxy;
import com.serotonin.m2m2.db.MySQLProxy;
import com.serotonin.m2m2.db.PostgresProxy;

@Component
public class DefaultDatabaseProxyFactory implements DatabaseProxyFactory {

    private final Environment env;

    @Autowired
    public DefaultDatabaseProxyFactory(Environment env) {
        this.env = env;
    }

    @Override
    public AbstractDatabaseProxy createDatabaseProxy(DatabaseType type) {
        boolean useMetrics = env.getProperty("db.useMetrics", boolean.class, false);

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
