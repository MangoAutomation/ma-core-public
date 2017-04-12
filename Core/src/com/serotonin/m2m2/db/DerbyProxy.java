/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.db;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.derby.jdbc.EmbeddedXADataSource40;
import org.apache.derby.tools.ij;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.datasource.DataSourceUtils;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.DaoUtils;
import com.serotonin.db.spring.ConnectionCallbackVoid;
import com.serotonin.db.spring.ExtendedJdbcTemplate;
import com.serotonin.m2m2.Common;
import com.serotonin.util.DirectoryInfo;
import com.serotonin.util.DirectoryUtils;
import com.serotonin.util.StringUtils;

/**
 * Support for Derby removed in 3.0.0+
 * 
 * @author Terry Packer
 */
@Deprecated
public class DerbyProxy extends DatabaseProxy {
    private final Log log = LogFactory.getLog(DerbyProxy.class);

    private static final double LARGEST_POSITIVE = 1.79769E+308;
    private static final double SMALLEST_POSITIVE = 2.225E-307;
    private static final double LARGEST_NEGATIVE = -2.225E-307;
    private static final double SMALLEST_NEGATIVE = -1.79769E+308;

    private EmbeddedXADataSource40 dataSource;

    @Override
    public DatabaseType getType() {
        return DatabaseType.DERBY;
    }

    @Override
    protected void initializeImpl(String propertyPrefix) {
        log.info("Initializing derby connection manager");
        dataSource = new EmbeddedXADataSource40();
        dataSource.setCreateDatabase("create");

        dataSource.setDatabaseName(getUrl(propertyPrefix));
        dataSource.setDataSourceName("mangoDataSource");

        // Creation of a connection will optionally create the database.
        Connection c = DataSourceUtils.getConnection(dataSource);
        DataSourceUtils.releaseConnection(c, dataSource);
    }

    private String getUrl(String propertyPrefix) {
        String url = Common.envProps.getString(propertyPrefix + "db.url", "derby");
        url = StringUtils.replaceMacros(url, System.getProperties());
        return url;
    }

    @Override
    public void terminateImpl() {
        log.info("Stopping database");
        dataSource.setDatabaseName("");
        dataSource.setShutdownDatabase("shutdown");
        Connection conn = null;
        try {
            conn = DataSourceUtils.getConnection(dataSource);
        }
        catch (CannotGetJdbcConnectionException e) {
            SQLException se = (SQLException) e.getCause();
            if ("XJ015".equals(se.getSQLState())) {
                log.debug("Stopped database");
                // A SQL code indicating that the system was successfully shut down. We can ignore this.
            }
            else
                throw e;
        }
        DataSourceUtils.releaseConnection(conn, dataSource);
    }

    @Override
    public DataSource getDataSource() {
        return dataSource;
    }

    @Override
    protected void postInitialize(ExtendedJdbcTemplate ejt) {
        // This code updates the tables with identity autoincrement values that are the maximum of the current
        // autoincrement value or max(id)+1. This ensures that updates to tables that may have occurred are handled, and
        // prevents cases where inserts are attempted with identities that already exist.
        List<IdentityStart> starts = ejt.query("select t.tablename, c.columnname, c.autoincrementvalue " + //
                "from sys.syscolumns c join sys.systables t on c.referenceid = t.tableid " + //
                "where t.tabletype='T' and c.autoincrementvalue is not null", new RowMapper<IdentityStart>() {
            @Override
            public IdentityStart mapRow(ResultSet rs, int index) throws SQLException {
                IdentityStart is = new IdentityStart();
                is.table = rs.getString(1);
                is.column = rs.getString(2);
                is.aiValue = rs.getInt(3);
                return is;
            }
        });

        for (IdentityStart is : starts) {
        	int maxId = ejt.queryForObject("select max(" + is.column + ") from " + is.table, new Object[]{}, Integer.class, 0);
            if (is.aiValue <= maxId)
                ejt.execute("alter table " + is.table + " alter column " + is.column + " restart with " + (maxId + 1));
        }
    }

    class IdentityStart {
        String table;
        String column;
        int aiValue;
    }

    @Override
    public void runScript(String[] script, final OutputStream out) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (String line : script)
            sb.append(line).append("\r\n");
        runScript(new ByteArrayInputStream(sb.toString().getBytes("ASCII")), out);
    }

    @Override
    public void runScript(final InputStream in, OutputStream out) {
        final OutputStream finalOut;
        if (out == null)
            finalOut = new ByteArrayOutputStream();
        else
            finalOut = out;

        Common.databaseProxy.doInConnection(new ConnectionCallbackVoid() {
            @Override
            public void doInConnection(Connection conn) {
                try {
                    ij.runScript(conn, in, "ASCII", finalOut, Common.UTF8);
                }
                catch (UnsupportedEncodingException e) {
                    throw new ShouldNeverHappenException(e);
                }
            }
        });

        if (finalOut != out) {
            ByteArrayOutputStream baos = (ByteArrayOutputStream) finalOut;
            String s = new String(baos.toByteArray(), Common.UTF8_CS);
            log.info("Derby script output: " + s);
        }
    }

    @Override
    public File getDataDirectory() {
    	String dataDir = getUrl("");
    	return new File(dataDir);
    	
    }
    
    @Override
    public Long getDatabaseSizeInBytes(){
    	String dataDir = getUrl("");
    	File dbData = new File(dataDir);
    	if(dbData.exists()){
    		DirectoryInfo dbInfo = DirectoryUtils.getSize(dbData);
    		return dbInfo.getSize();
    	}else
    		return null;
    }

    @Override
    public double applyBounds(double value) {
        if (value > 0) {
            if (value < SMALLEST_POSITIVE)
                value = SMALLEST_POSITIVE;
            else if (value > LARGEST_POSITIVE)
                value = LARGEST_POSITIVE;
        }
        else if (value < 0) {
            if (value < SMALLEST_NEGATIVE)
                value = SMALLEST_NEGATIVE;
            else if (value > LARGEST_NEGATIVE)
                value = LARGEST_NEGATIVE;
        }
        else if (Double.isNaN(value))
            value = 0;

        return value;
    }

    @Override
    public void executeCompress(ExtendedJdbcTemplate ejt) {
        compressTable(ejt, "pointValues");
        compressTable(ejt, "pointValueAnnotations");
        compressTable(ejt, "events");
    }

    private void compressTable(ExtendedJdbcTemplate ejt, final String tableName) {
        List<SqlParameter> list = Collections.emptyList();
        ejt.call(new CallableStatementCreator() {
            @Override
            public CallableStatement createCallableStatement(Connection conn) throws SQLException {
                CallableStatement cs = conn.prepareCall("call SYSCS_UTIL.SYSCS_COMPRESS_TABLE(?, ?, ?)");
                cs.setString(1, "APP");
                cs.setString(2, tableName.toUpperCase());
                cs.setShort(3, (short) 0);
                return cs;
            }
        }, list);
    }

    //    @Override
    //    public String getPaginationSql(int offset, int count) {
    //        return " offset " + offset + " rows fetch next " + count + " rows only";
    //    }

    @Override
    public boolean tableExists(ExtendedJdbcTemplate ejt, String tableName) {
        return ejt.queryForObject("select count(1) from sys.systables where tablename='" + tableName.toUpperCase() + "'", new Object[]{}, Integer.class, 0) > 0;
    }

    @Override
    public <T> List<T> doLimitQuery(DaoUtils dao, String sql, Object[] args, RowMapper<T> rowMapper, int limit) {
        return dao.query(getLimitQuerySql(sql, limit), args, rowMapper);
    }

    private String getLimitQuerySql(String sql, int limit) {
        if (limit > 0)
            sql += " fetch first " + limit + " rows only";
        return sql;
    }

    @Override
    public String getTableListQuery() {
        return "select TABLENAME from sys.systables where tabletype='T' order by TABLENAME";
    }

    @Override
    public int getActiveConnections() {
        return -1;
    }

    @Override
    public int getIdleConnections() {
        return -1;
    }

    @Override
    protected String getLimitDelete(String sql, int chunkSize) {
        return sql;
    }
}
