/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.db;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.MissingResourceException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import com.infiniteautomation.mango.db.tables.Users;
import com.infiniteautomation.mango.util.NullOutputStream;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.spring.ExtendedJdbcTemplate;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.PointValueCacheDao;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.db.dao.PointValueDaoMetrics;
import com.serotonin.m2m2.db.dao.PointValueDaoSQL;
import com.serotonin.m2m2.db.upgrade.DBUpgrade;
import com.serotonin.m2m2.module.DatabaseSchemaDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;

abstract public class AbstractDatabaseProxy implements DatabaseProxy {

    private final Log log = LogFactory.getLog(AbstractDatabaseProxy.class);
    private NoSQLProxy noSQLProxy;
    private PointValueCacheProxy pointValueCacheProxy;
    private final boolean useMetrics;
    private PlatformTransactionManager transactionManager;
    private final DatabaseProxyFactory factory;

    public AbstractDatabaseProxy(DatabaseProxyFactory factory, boolean useMetrics) {
        this.factory = factory;
        this.useMetrics = useMetrics;
    }

    @Override
    public void initialize(ClassLoader classLoader) {
        initializeImpl("");

        ExtendedJdbcTemplate ejt = new ExtendedJdbcTemplate();
        ejt.setDataSource(getDataSource());
        DSLContext context = DSL.using(getConfig());

        transactionManager = new DataSourceTransactionManager(getDataSource());

        //First confirm that if we are MySQL we have JSON Support
        if(getType().name().equals(DatabaseType.MYSQL.name())) {
            try {
                runScript(new String[] {"CREATE TABLE mangoUpgrade28 (test JSON)engine=InnoDB;", "DROP TABLE mangoUpgrade28;"}, null);
            }catch(BadSqlGrammarException e) {
                String version = "?";
                try {
                    DatabaseMetaData dmd = getDataSource().getConnection().getMetaData();
                    version = dmd.getDatabaseProductVersion();
                }catch(Exception ex) {
                    log.error("Failed to create test table for JSON compatibility" + ex);
                }
                throw new ShouldNeverHappenException("Unable to start Mango, MySQL version must be at least 5.7.8 to support JSON columns. Your version is " + version);
            }
        }

        try {
            if (newDatabaseCheck(ejt)) {
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

                    AbstractDatabaseProxy sourceProxy = getFactory().createDatabaseProxy(convertType);
                    sourceProxy.initializeImpl("convert.");
                    try {
                        DBConvert convert = new DBConvert();
                        convert.setSource(sourceProxy);
                        convert.setTarget(this);
                        try {
                            convert.execute();
                        } catch (SQLException e) {
                            throw new ShouldNeverHappenException(e);
                        }
                    } finally {
                        sourceProxy.terminate(false);
                    }
                } else {
                    doInTransaction(txStatus -> {
                        initializeCoreDatabase(context);
                    });
                }
            } else {
                // The database exists, so let's make its schema version matches the application version.
                DBUpgrade.checkUpgrade();
            }

            //Ensure the modules are upgraded/installed after the core schema is updated
            for (DatabaseSchemaDefinition def : ModuleRegistry.getDefinitions(DatabaseSchemaDefinition.class)) {
                try {
                    def.newInstallationCheck(ejt);
                } catch (Exception e) {
                    log.error("Module " + def.getModule().getName() + " new installation check failed", e);
                }
            }

            // Check if we are using NoSQL and use the first enabled proxy
            List<NoSQLProxy> proxies = ModuleRegistry.getDefinitions(NoSQLProxy.class);
            for(NoSQLProxy proxy : proxies) {
                if (proxy.isEnabled()) {
                    noSQLProxy = proxy;
                    noSQLProxy.initialize();
                    break;
                }
            }

            //Check to see if we are using the latest values store
            List<PointValueCacheProxy> latestValueProxies = ModuleRegistry.getDefinitions(PointValueCacheProxy.class);
            for(PointValueCacheProxy proxy : latestValueProxies) {
                if (proxy.isEnabled()) {
                    pointValueCacheProxy = proxy;
                    //Defer initialization until post spring context init via module element definition lifecycle
                    break;
                }
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
    }

    private boolean newDatabaseCheck(ExtendedJdbcTemplate ejt) {
        boolean coreIsNew = false;

        if (!tableExists(ejt, Users.USERS.getName())) {
            // The users table wasn't found, so assume that this is a new instance.
            // Create the tables
            try {
                String scriptName = "createTables-" + getType().name() + ".sql";
                try (InputStream resource = AbstractDatabaseProxy.class.getResourceAsStream(scriptName)) {
                    if (resource == null) {
                        throw new ShouldNeverHappenException("Could not get script " + scriptName + " for class " + AbstractDatabaseProxy.class.getName());
                    }

                    try (OutputStream os = createLogOutputStream("createTables.log")) {
                        runScript(resource, os);
                    }
                }
            } catch (IOException e) {
                throw new ShouldNeverHappenException(e);
            }
            coreIsNew = true;
        }

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

    abstract protected void terminateImpl();

    /**
     * Should create the DataSource, does not do conversion etc
     * @param propertyPrefix string to prefix in front of env.properties when getting connection URL etc
     */
    abstract protected void initializeImpl(String propertyPrefix);

    @Override
    public String getDatabasePassword(String propertyPrefix) {
        return Common.envProps.getString(propertyPrefix + "db.password");
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
    public PointValueCacheProxy getPointValueCacheProxy() {
        return pointValueCacheProxy;
    }

    @Override
    public PointValueCacheDao getPointValueCacheDao() { return pointValueCacheProxy.getDao(); }

    @Override
    public PlatformTransactionManager getTransactionManager() {
        return transactionManager;
    }

    @Override
    public OutputStream createLogOutputStream(String fileName) {
        String dir = Common.envProps.getString("db.update.log.dir", "");
        Path logPath = Common.getLogsPath().resolve(dir).toAbsolutePath().normalize();
        Path logFile = logPath.resolve(fileName);

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

    @Override
    public boolean isUseMetrics() {
        return this.useMetrics;
    }

    protected DatabaseProxyFactory getFactory() {
        return this.factory;
    }
}
