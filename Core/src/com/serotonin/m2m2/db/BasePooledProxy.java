/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.m2m2.Common;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * @author Matthew Lohbihler
 */
abstract public class BasePooledProxy extends AbstractDatabaseProxy {
    private final Logger log = LoggerFactory.getLogger(BasePooledProxy.class);
    private HikariDataSource dataSource;

    public BasePooledProxy(DatabaseProxyFactory factory, boolean useMetrics) {
        super(factory, useMetrics);
    }

    @Override
    protected void initializeImpl(String propertyPrefix) {
        log.info("Initializing pooled connection manager");
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(getUrl(propertyPrefix));
        config.setUsername(Common.envProps.getString(propertyPrefix + "db.username"));
        config.setPassword(getDatabasePassword(propertyPrefix));
        config.setDriverClassName(getDriverClassName());
        config.setConnectionTestQuery("SELECT 1");
        config.setMaximumPoolSize(Common.envProps.getInt(propertyPrefix + "db.pool.maxActive", 10));
        config.setMinimumIdle(Common.envProps.getInt(propertyPrefix + "db.pool.maxIdle", 10));
        dataSource = new HikariDataSource(config);
    }

    protected String getUrl(String propertyPrefix) {
        return Common.envProps.getString(propertyPrefix + "db.url");
    }

    abstract protected String getDriverClassName();

    @Override
    public void terminateImpl() {
        log.info("Stopping database");
        dataSource.close();
    }

    @Override
    public DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public int getActiveConnections() {
        return dataSource.getHikariPoolMXBean().getActiveConnections();
    }

    @Override
    public int getIdleConnections() {
        return dataSource.getHikariPoolMXBean().getIdleConnections();
    }
}
