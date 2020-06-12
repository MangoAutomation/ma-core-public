/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.db;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.MissingResourceException;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.PlatformTransactionManager;

import com.infiniteautomation.mango.util.NullOutputStream;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.DaoUtils;
import com.serotonin.db.spring.ConnectionCallbackVoid;
import com.serotonin.db.spring.ExtendedJdbcTemplate;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.db.dao.PointValueDaoMetrics;
import com.serotonin.m2m2.db.dao.PointValueDaoSQL;
import com.serotonin.m2m2.db.dao.SchemaDefinition;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.db.upgrade.DBUpgrade;
import com.serotonin.m2m2.module.DatabaseSchemaDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;

abstract public class AbstractDatabaseProxy implements DatabaseProxy {

    public static DatabaseProxy createDatabaseProxy() {
        String type = Common.envProps.getString("db.type", "h2");
        DatabaseType dt = DatabaseType.valueOf(type.toUpperCase());
        return dt.getImpl();
    }

    private final Log log = LogFactory.getLog(AbstractDatabaseProxy.class);
    private NoSQLProxy noSQLProxy;
    private Boolean useMetrics;
    private PlatformTransactionManager transactionManager;

    @Override
    public void initialize(ClassLoader classLoader) {
        initializeImpl("");

        useMetrics = Common.envProps.getBoolean("db.useMetrics", false);

        ExtendedJdbcTemplate ejt = new ExtendedJdbcTemplate();
        ejt.setDataSource(getDataSource());

        transactionManager = new DataSourceTransactionManager(getDataSource());

        //First confirm that if we are MySQL we have JSON Support
        if(getType().name().equals(DatabaseProxy.DatabaseType.MYSQL.name())) {
            try {
                runScript(new String[] {"CREATE TABLE mangoUpgrade28 (test JSON)engine=InnoDB;", "DROP TABLE mangoUpgrade28;"}, null);
            }catch(BadSqlGrammarException e) {
                String version = "unknown";
                try {
                    DatabaseMetaData dmd = getDataSource().getConnection().getMetaData();
                    version = dmd.getDatabaseProductVersion();
                }catch(Exception ex) {
                    //Munch
                    ex.printStackTrace();
                }
                throw new ShouldNeverHappenException("Unable to start Mango, MySQL version must be at least 5.7.8 to support JSON columns. You have " + version);
            }
        }

        boolean newDatabase = false;
        try {
            if (newDatabaseCheck(ejt)) {
                newDatabase = true;
                // Check if we should convert from another database.
                String convertTypeStr = null;
                try {
                    convertTypeStr = Common.envProps.getString("convert.db.type");
                }
                catch (MissingResourceException e) {
                    convertTypeStr = "";
                }

                if (!StringUtils.isBlank(convertTypeStr)) {
                    // Found a database type from which to convert.
                    DatabaseType convertType = DatabaseType.valueOf(convertTypeStr.toUpperCase());

                    // TODO check that the convert source has the current DB version, or upgrade it if not.

                    AbstractDatabaseProxy sourceProxy = convertType.getImpl();
                    sourceProxy.initializeImpl("convert.");

                    DBConvert convert = new DBConvert();
                    convert.setSource(sourceProxy);
                    convert.setTarget(this);
                    try {
                        convert.execute();
                    }
                    catch (SQLException e) {
                        throw new ShouldNeverHappenException(e);
                    }

                    sourceProxy.terminate(false);
                }
                else {

                    // Record the current version.
                    SystemSettingsDao.instance.setValue(SystemSettingsDao.DATABASE_SCHEMA_VERSION,
                            Integer.toString(Common.getDatabaseSchemaVersion()));

                    // Add the settings flag that this is a new instance. This flag is removed when an administrator
                    // logs in.
                    SystemSettingsDao.instance.setBooleanValue(SystemSettingsDao.NEW_INSTANCE, true);
                }
            }
            else
                // The database exists, so let's make its schema version matches the application version.
                DBUpgrade.checkUpgrade();

            // Check if we are using NoSQL
            NoSQLProxy proxy = ModuleRegistry.getDefinition(NoSQLProxy.class);
            if (proxy != null) {
                noSQLProxy = proxy;
                noSQLProxy.initialize();
            }

        }
        catch (CannotGetJdbcConnectionException e) {
            log.fatal("Unable to connect to database of type " + getType().name(), e);
            throw e;
        }
        catch (Exception e) {
            log.fatal("Exception initializing database proxy: " + e.getMessage(), e);
            throw e;
        }

        // Allow modules to upgrade their schemas
        for (DatabaseSchemaDefinition def : ModuleRegistry.getDefinitions(DatabaseSchemaDefinition.class))
            DBUpgrade.checkUpgrade(def, classLoader);

        postInitialize(ejt, "", newDatabase);
    }

    //TODO Mango 4.0 it seems possible that if a core is installed, new modules can be installed and THEN the core and modules will be upgraded.
    private boolean newDatabaseCheck(ExtendedJdbcTemplate ejt) {
        boolean coreIsNew = false;

        if (!tableExists(ejt, SchemaDefinition.USERS_TABLE)) {
            // The users table wasn't found, so assume that this is a new instance.
            // Create the tables
            try {
                String scriptName = "createTables-" + getType().name() + ".sql";
                try (InputStream resource = AbstractDatabaseProxy.class.getResourceAsStream(scriptName)) {
                    if (resource == null) {
                        throw new ShouldNeverHappenException("Could not get script " + scriptName + " for class " + AbstractDatabaseProxy.class.getName());
                    }
                    try (OutputStream os = new FileOutputStream(new File(Common.getLogsDir(), "createTables.log"))) {
                        runScript(resource, os);
                    }
                }
            } catch (IOException e) {
                throw new ShouldNeverHappenException(e);
            }
            coreIsNew = true;
        }

        for (DatabaseSchemaDefinition def : ModuleRegistry.getDefinitions(DatabaseSchemaDefinition.class))
            def.newInstallationCheck(ejt);

        return coreIsNew;
    }

    @Override
    public void terminate(boolean terminateNoSql) {
        terminateImpl();
        // Check if we are using NoSQL
        if ((terminateNoSql)&&(noSQLProxy != null)) {
            noSQLProxy.shutdown();
        }
    }

    abstract protected void initializeImpl(String propertyPrefix);

    protected void postInitialize(ExtendedJdbcTemplate ejt, String propertyPrefix, boolean newDatabase) {
        // no op - override as necessary
    }

    @Override
    public void doInConnection(ConnectionCallbackVoid callback) {
        DataSource dataSource = getDataSource();
        Connection conn = null;
        try {
            conn = DataSourceUtils.getConnection(dataSource);
            conn.setAutoCommit(false);
            callback.doInConnection(conn);
            conn.commit();
        }
        catch (Exception e) {
            try {
                if (conn != null)
                    conn.rollback();
            }
            catch (SQLException e1) {
                log.warn("Exception during rollback", e1);
            }

            // Wrap and rethrow
            throw new ShouldNeverHappenException(e);
        }
        finally {
            if (conn != null)
                DataSourceUtils.releaseConnection(conn, dataSource);
        }
    }

    @Override
    abstract public <T> List<T> doLimitQuery(DaoUtils dao, String sql, Object[] args, RowMapper<T> rowMapper, int limit);

    @Override
    public long doLimitDelete(ExtendedJdbcTemplate ejt, String sql, Object[] args, int chunkSize, int chunkWait,
            int limit) {
        sql = getLimitDelete(sql, chunkSize);

        long total = 0;
        while (true) {
            int cnt;
            if (args == null)
                cnt = ejt.update(sql);
            else
                cnt = ejt.update(sql, args);

            total += cnt;

            if (cnt < chunkSize || (limit > 0 && total >= limit))
                break;

            if (chunkWait > 0) {
                try {
                    Thread.sleep(chunkWait);
                }
                catch (InterruptedException e) {
                    // no op
                }
            }
        }

        return total;
    }

    abstract protected String getLimitDelete(String sql, int chunkSize);

    @Override
    public String getDatabasePassword(String propertyPrefix) {
        String input = Common.envProps.getString(propertyPrefix + "db.password");
        return new DatabaseAccessUtils().decrypt(input);
    }

    @Override
    public void setNoSQLProxy(NoSQLProxy proxy) {
        this.noSQLProxy = proxy;
    }

    @Override
    public PointValueDao newPointValueDao() {
        if (noSQLProxy == null) {
            if (useMetrics)
                return new PointValueDaoMetrics(new PointValueDaoSQL());
            return new PointValueDaoSQL();
        }

        if (useMetrics)
            return noSQLProxy.createPointValueDaoMetrics();
        return noSQLProxy.createPointValueDao();
    }

    @Override
    public NoSQLProxy getNoSQLProxy() {
        return noSQLProxy;
    }

    @Override
    public PlatformTransactionManager getTransactionManager() {
        return transactionManager;
    }

    @Override
    public OutputStream createLogOutputStream(Class<?> clazz) {
        String dir = Common.envProps.getString("db.update.log.dir", "");
        Path logPath = Common.getLogsPath().resolve(dir).toAbsolutePath().normalize();
        Path logFile = logPath.resolve(clazz.getName() + ".log");

        try {
            Files.createDirectories(logPath);
            log.info("Writing upgrade log to " + logFile.toString());
            return Files.newOutputStream(logFile, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            log.error("Failed to create database upgrade log file.", e);
        }

        log.warn("Failing over to null output stream, database upgrade messages will be lost");
        return new NullOutputStream();
    }
}
