package com.serotonin.m2m2.db;

import java.io.File;
import java.sql.SQLException;
import java.util.List;

import org.postgresql.util.PSQLException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.RowMapper;

import com.serotonin.db.DaoUtils;
import com.serotonin.db.spring.ExtendedJdbcTemplate;

public class PostgresProxy extends BasePooledProxy {
    public PostgresProxy(DatabaseProxyFactory factory, boolean useMetrics) {
        super(factory, useMetrics);
    }

    @Override
    public DatabaseType getType() {
        return DatabaseType.POSTGRES;
    }

    @Override
    protected String getDriverClassName() {
        return "org.postgresql.Driver";
    }

    @Override
    public double applyBounds(double value) {
        return value;
    }

    @Override
    public void executeCompress(ExtendedJdbcTemplate ejt) {
        // no op
    }

    @Override
    public boolean tableExists(ExtendedJdbcTemplate ejt, String tableName) {
        try {
            ejt.execute("SELECT COUNT(*) FROM " + tableName);
        }
        catch (BadSqlGrammarException e) {
            if (e.getCause() instanceof PSQLException) {
                SQLException se = (SQLException) e.getCause();
                // This state means a missing table.
                return !"42P01".equals(se.getSQLState());
            }
            throw e;
        }
        return true;
    }

    @Override
    public <T> List<T> doLimitQuery(DaoUtils dao, String sql, Object[] args, RowMapper<T> rowMapper, int limit) {
        if (limit > 0)
            sql += " LIMIT " + limit;
        return dao.query(sql, args, rowMapper);
    }

    @Override
    public String getTableListQuery() {
        return "SELECT table_name FROM information_schema.tables "
                + "WHERE table_catalog=current_database() AND table_schema=current_schema()";
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
