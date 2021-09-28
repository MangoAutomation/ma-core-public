/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import com.infiniteautomation.mango.db.tables.Users;
import com.infiniteautomation.mango.spring.DatabaseProxyConfiguration;
import com.infiniteautomation.mango.spring.DatabaseProxyConfiguration.DatabaseProxyListener;
import com.infiniteautomation.mango.util.NullOutputStream;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.spring.ExtendedJdbcTemplate;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.db.upgrade.DatabaseSchemaUpgrader;
import com.serotonin.m2m2.db.upgrade.SystemSettingsAccessor;
import com.serotonin.m2m2.module.DatabaseSchemaDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;

abstract public class AbstractDatabaseProxy implements DatabaseProxy {

    private final Logger log = LoggerFactory.getLogger(AbstractDatabaseProxy.class);
    protected final DatabaseProxyFactory factory;
    protected final Environment env;
    protected final ClassLoader classLoader;
    protected final List<DatabaseProxyListener> listeners;

    private PlatformTransactionManager transactionManager;
    private DSLContext context;
    private ExtendedJdbcTemplate jdbcTemplate;

    public AbstractDatabaseProxy(DatabaseProxyFactory factory, DatabaseProxyConfiguration configuration) {
        this.factory = factory;
        this.env = configuration.getEnv();
        this.classLoader = configuration.getClassLoader();
        this.listeners = configuration.getListeners();
    }

    @PostConstruct
    @Override
    public void initialize() {
        initializeImpl("");

        DataSource dataSource = getDataSource();

        this.jdbcTemplate = new ExtendedJdbcTemplate(dataSource);
        this.context = DSL.using(getConfig());

        this.transactionManager = new DataSourceTransactionManager(dataSource);
        SystemSettingsAccessor systemSettingsAccessor = () -> context;
        DatabaseSchemaUpgrader upgrader = new DatabaseSchemaUpgrader(this,
                systemSettingsAccessor,
                classLoader);

        // we have to set the locale before even attempting upgrades as the locale/language is used when deserializing
        // VO objects stored in blobs
        try {
            systemSettingsAccessor.getSystemSetting(SystemSettingsDao.LANGUAGE).ifPresent(Common::setSystemLanguage);
        } catch (DataAccessException e) {
            // that's ok, table probably doesn't exist yet
        }

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
            boolean newDatabase = false;
            String convertTypeStr = env.getProperty("convert.db.type");
            boolean willConvert = false;

            if (!databaseExists()) {
                if (!restoreTables()) {
                    createTables();
                    // Check if we should convert from another database.
                    if (!StringUtils.isBlank(convertTypeStr)) {
                        willConvert = true;
                    }else {
                        newDatabase = true;
                    }
                }
            }

            if(newDatabase){
                doInTransaction(txStatus -> {
                    initializeCoreDatabase(context);
                });
            }else if(!willConvert) {
                // Make sure the core schema version matches the application version.  If we are running a conversion
                // then the responsibility is on the User to be converting from a compatible version
                upgrader.checkCoreUpgrade();
            }

            //Ensure the modules are installed after the core schema is updated
            for (DatabaseSchemaDefinition def : ModuleRegistry.getDefinitions(DatabaseSchemaDefinition.class)) {
                try {
                    def.setDatabaseProxy(this);
                    def.newInstallationCheck();
                } catch (Exception e) {
                    log.error("Module " + def.getModule().getName() + " new installation check failed", e);
                }
            }

            if(!willConvert) {
                // Allow modules to upgrade their schemas
                for (DatabaseSchemaDefinition def : ModuleRegistry.getDefinitions(DatabaseSchemaDefinition.class)) {
                    upgrader.checkModuleUpgrade(def);
                }
            }

            if(willConvert) {
                // Found a database type from which to convert.
                DatabaseType convertType = DatabaseType.valueOf(convertTypeStr.toUpperCase());

                // TODO check that the convert source has the current DB version, or upgrade it if not.

                AbstractDatabaseProxy sourceProxy = (AbstractDatabaseProxy) factory.createDatabaseProxy(convertType);
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
                    sourceProxy.terminate();
                }
            }

            listeners.forEach(l -> l.onInitialize(this));
        } catch (CannotGetJdbcConnectionException e) {
            log.error("Unable to connect to database of type " + getType().name(), e);
            throw e;
        } catch (IOException e) {
            log.error("Exception initializing database proxy: " + e.getMessage(), e);
            throw new UncheckedIOException(e);
        } catch (Exception e) {
            log.error("Exception initializing database proxy: " + e.getMessage(), e);
            throw e;
        }
    }

    protected boolean databaseExists() {
        return tableExists(Users.USERS.getName());
    }

    protected void createTables() throws IOException {
        String scriptName = "createTables-" + getType().name() + ".sql";
        try (InputStream resource = AbstractDatabaseProxy.class.getResourceAsStream(scriptName)) {
            if (resource == null) {
                throw new ShouldNeverHappenException("Could not open script " + scriptName);
            }
            try (OutputStream os = createTablesOutputStream()) {
                runScript(resource, os);
            }
        }
    }

    /**
     * @return true if database was restored
     * @throws IOException
     */
    protected boolean restoreTables() throws IOException {
        String restoreFrom = env.getProperty("db.createTables.restoreFrom");
        if (restoreFrom == null) {
            return false;
        }

        Path restoreFromPath = Common.MA_DATA_PATH.resolve(restoreFrom).normalize();
        try (OutputStream os = createTablesOutputStream()) {
            if (restoreFromPath.getFileName().toString().endsWith(".zip")) {
                try (ZipFile zip = new ZipFile(restoreFromPath.toFile())) {
                    ZipEntry script = null;

                    Enumeration<? extends ZipEntry> entries = zip.entries();
                    while (entries.hasMoreElements()) {
                        ZipEntry entry = entries.nextElement();
                        if (!entry.isDirectory() && entry.getName().endsWith(".sql")) {
                            script = entry;
                        }
                    }

                    if (script == null) {
                        throw new IllegalStateException("Zip file does not contain a .sql script");
                    }

                    try (InputStream resource = zip.getInputStream(script)) {
                        runScript(resource, os);
                    }
                }
            } else {
                try (InputStream resource = Files.newInputStream(restoreFromPath)) {
                    runScript(resource, os);
                }
            }
        }
        return true;
    }

    protected OutputStream createTablesOutputStream() {
        boolean createLogFile = env.getProperty("db.createTables.createLogFile", boolean.class, true);
        return createLogFile ? createLogOutputStream("createTables.log") : null;
    }

    @PreDestroy
    @Override
    public void terminate() {
        listeners.forEach(l -> l.onTerminate(this));
        terminateImpl();
        this.context = null;
        this.transactionManager = null;
        this.jdbcTemplate = null;
    }

    abstract protected void terminateImpl();

    /**
     * Should create the DataSource, does not do conversion etc
     * @param propertyPrefix string to prefix in front of property name when getting connection URL etc
     */
    abstract protected void initializeImpl(String propertyPrefix);

    @Override
    public String getDatabasePassword(String propertyPrefix) {
        return env.getProperty(propertyPrefix + "db.password");
    }

    @Override
    public PlatformTransactionManager getTransactionManager() {
        return transactionManager;
    }

    @Override
    public OutputStream createLogOutputStream(String fileName) {
        String dir = env.getProperty("db.update.log.dir", "");
        Path logPath = Common.getLogsPath().resolve(dir).toAbsolutePath().normalize();
        Path logFile = logPath.resolve(fileName);

        try {
            Files.createDirectories(logPath);
            log.info("Writing upgrade log to " + logFile);
            return Files.newOutputStream(logFile, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            log.error("Failed to create database upgrade log file.", e);
        }

        log.warn("Failing over to null output stream, database upgrade messages will be lost");
        return new NullOutputStream();
    }

    @Override
    public boolean isUseMetrics() {
        return env.getProperty("db.useMetrics", boolean.class, false);
    }

    @Override
    public long metricsThreshold() {
        return env.getProperty("db.metricsThreshold", long.class, 0L);
    }

    @Override
    public DSLContext getContext() {
        return context;
    }

    @Override
    public ExtendedJdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    @Override
    public int batchSize() {
        return env.getProperty("db.batchSize", int.class, DatabaseProxy.super.batchSize());
    }

    @Override
    public int maxInParameters() {
        return env.getProperty("db.maxInParameters", int.class, DatabaseProxy.super.maxInParameters());
    }
}
