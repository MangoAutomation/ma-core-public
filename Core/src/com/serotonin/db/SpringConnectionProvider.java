/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.db;

import java.sql.Connection;

import javax.sql.DataSource;

import org.jooq.ConnectionProvider;
import org.jooq.exception.DataAccessException;
import org.springframework.jdbc.datasource.DataSourceUtils;

/**
 * Provides connections to jOOQ from a data source via Spring's DataSourceUtils.getConnection()
 * so that it receives the correct connection when we are inside a transaction.
 * 
 * @author Jared Wiltshire
 */
public class SpringConnectionProvider implements ConnectionProvider {
    final DataSource dataSource;
    
    public SpringConnectionProvider(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Connection acquire() throws DataAccessException {
        return DataSourceUtils.getConnection(dataSource);
    }

    @Override
    public void release(Connection connection) throws DataAccessException {
        DataSourceUtils.releaseConnection(connection, dataSource);
    }

}
