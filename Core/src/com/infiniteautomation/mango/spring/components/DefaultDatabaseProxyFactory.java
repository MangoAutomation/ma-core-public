/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring.components;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.spring.DatabaseProxyConfiguration;
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

    private final DatabaseProxyConfiguration configuration;

    @Autowired
    public DefaultDatabaseProxyFactory(DatabaseProxyConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public AbstractDatabaseProxy createDatabaseProxy(DatabaseType type, String propertyPrefix) {
        switch(type) {
            case H2:
                return new H2Proxy(this, configuration, propertyPrefix);
            case MSSQL:
                return new MSSQLProxy(this, configuration, propertyPrefix);
            case MYSQL:
                return new MySQLProxy(this, configuration, propertyPrefix);
            case POSTGRES:
                return new PostgresProxy(this, configuration, propertyPrefix);
            default:
                throw new ShouldNeverHappenException("Unknown/unsupported database type " + type);
        }
    }

}
