/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db;

import java.sql.SQLException;

import org.springframework.dao.DataAccessException;

import com.infiniteautomation.mango.spring.DatabaseProxyConfiguration;
import com.serotonin.db.spring.ExtendedJdbcTemplate;

public class MSSQLProxy extends BasePooledProxy {
    public MSSQLProxy(DatabaseProxyFactory factory, DatabaseProxyConfiguration configuration) {
        super(factory, configuration);
    }

    @Override
    public DatabaseType getType() {
        return DatabaseType.MSSQL;
    }

    @Override
    protected String getDriverClassName() {
        return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    }

    @Override
    public boolean tableExists(ExtendedJdbcTemplate ejt, String tableName) {
        try {
            ejt.execute("select count(*) from " + tableName);
        }
        catch (DataAccessException e) {
            if (e.getCause() instanceof SQLException) {
                SQLException se = (SQLException) e.getCause();
                // This state means a missing table.
                return !"S0002".equals(se.getSQLState());
            }
            throw e;
        }
        return true;
    }

    @Override
    public double applyBounds(double value) {
        if (Double.isNaN(value))
            return 0;
        if (value == Double.POSITIVE_INFINITY)
            return Double.MAX_VALUE;
        if (value == Double.NEGATIVE_INFINITY)
            return -Double.MAX_VALUE;

        return value;
    }

    @Override
    public String getTableListQuery() {
        return "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'BASE TABLE' order by table_name";
    }

    @Override
    public int batchInsertSize() {
        return 524;
    }
}
