/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.db;

import java.io.File;
import java.sql.SQLException;
import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowMapper;

import com.serotonin.db.DaoUtils;
import com.serotonin.db.spring.ExtendedJdbcTemplate;

public class MSSQLProxy extends BasePooledProxy {
    public MSSQLProxy(DatabaseProxyFactory factory, boolean useMetrics) {
        super(factory, useMetrics);
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
    public void executeCompress(ExtendedJdbcTemplate ejt) {
        // no op
    }

    @Override
    public <T> List<T> doLimitQuery(DaoUtils dao, String sql, Object[] args, RowMapper<T> rowMapper, int limit) {
        return dao.query(getLimitQuerySql(sql, limit), args, rowMapper);
    }

    private String getLimitQuerySql(String sql, int limit) {
        if (limit > 0) {
            if (sql.length() > 6 && sql.substring(0, 7).equalsIgnoreCase("select "))
                sql = "select top " + limit + " " + sql.substring(7);
        }
        return sql;
    }

    @Override
    public String getTableListQuery() {
        return "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'BASE TABLE' order by table_name";
    }

    @Override
    protected String getLimitDelete(String sql, int chunkSize) {
        return sql;
    }
    
    @Override
    public File getDataDirectory() {
    	return null; //TODO 
    	
    }
    
    @Override
    public Long getDatabaseSizeInBytes(){
    	return null;
    }
    
}
