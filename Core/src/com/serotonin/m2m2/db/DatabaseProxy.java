/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.db;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.PlatformTransactionManager;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.DaoUtils;
import com.serotonin.db.spring.ConnectionCallbackVoid;
import com.serotonin.db.spring.ExtendedJdbcTemplate;
import com.serotonin.m2m2.db.dao.PointValueDao;


/**
 *
 * @author Terry Packer
 */
public interface DatabaseProxy {

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

    void initialize(ClassLoader classLoader);

    DatabaseType getType();

    void terminate(boolean terminateNoSql);

    void terminateImpl();

    DataSource getDataSource();

    PlatformTransactionManager getTransactionManager();

    double applyBounds(double value);

    File getDataDirectory();

    /**
     *
     * @return size of Database in bytes or null if don't know
     */
    Long getDatabaseSizeInBytes();

    void executeCompress(ExtendedJdbcTemplate ejt);

    boolean tableExists(ExtendedJdbcTemplate ejt, String tableName);

    int getActiveConnections();

    int getIdleConnections();

    OutputStream createLogOutputStream(Class<?> clazz);

    void runScript(String[] script, OutputStream out);

    void runScript(InputStream in, OutputStream out);

    String getTableListQuery();

    void doInConnection(ConnectionCallbackVoid callback);

    <T> List<T> doLimitQuery(DaoUtils dao, String sql, Object[] args, RowMapper<T> rowMapper,
            int limit);

    long doLimitDelete(ExtendedJdbcTemplate ejt, String sql, Object[] args, int chunkSize,
            int chunkWait, int limit);

    String getDatabasePassword(String propertyPrefix);

    PointValueDao newPointValueDao();

    /**
     * TODO Mango 4.0 Remove this method (only used in testing and not necessary)
     * @param proxy
     */
    public void setNoSQLProxy(NoSQLProxy proxy);

    /**
     * Allow access to the NoSQL Proxy
     *
     * @return
     */
    NoSQLProxy getNoSQLProxy();

    /**
     * Get the latest value proxy
     * @return
     */
    LatestPointValueProxy getLatestPointValueProxy();

}
