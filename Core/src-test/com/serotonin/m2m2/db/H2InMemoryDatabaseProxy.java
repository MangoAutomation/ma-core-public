/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.UUID;
import java.util.function.Supplier;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcConnectionPool;
import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.Server;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import com.infiniteautomation.mango.db.tables.Users;
import com.infiniteautomation.mango.spring.service.CachingService;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.util.NullOutputStream;
import com.serotonin.db.spring.ExtendedJdbcTemplate;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.BaseDao;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.db.upgrade.DatabaseSchemaUpgrader;
import com.serotonin.m2m2.module.DatabaseSchemaDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;

/**
 * Using an H2 in memory database we can easily mock the database proxy.
 *
 * If you do not call initialize, none of the tables are created.  However
 *   it is possible to use the PointValueDao via the mock in memory list implementation.
 *
 * @author Terry Packer
 */
public class H2InMemoryDatabaseProxy implements DatabaseProxy {
    private final Logger log = LoggerFactory.getLogger(H2InMemoryDatabaseProxy.class);

    protected String databaseName = UUID.randomUUID().toString();
    protected JdbcConnectionPool dataSource;
    protected boolean initialized = false;
    protected final boolean initWebConsole;
    protected Integer webPort;
    private Server web; //web UI
    protected DataSourceTransactionManager transactionManager;
    protected final boolean useMetrics;
    protected final Supplier<InputStream> altCreateScript;
    protected final Supplier<InputStream> defaultDataScript;

    public H2InMemoryDatabaseProxy() {
        this(false, 0, false);
    }

    public H2InMemoryDatabaseProxy(boolean initWebConsole, Integer webPort) {
        this(initWebConsole, webPort, false);
    }

    public H2InMemoryDatabaseProxy(boolean initWebConsole, Integer webPort, boolean useMetrics) {
        this(initWebConsole, webPort, useMetrics, null, null);
    }

    public H2InMemoryDatabaseProxy(boolean initWebConsole, Integer webPort, boolean useMetrics, Supplier<InputStream> altCreateScript, Supplier<InputStream> defaultDataScript) {
        this.initWebConsole = initWebConsole;
        this.webPort = webPort;
        this.useMetrics = useMetrics;
        this.altCreateScript = altCreateScript;
        this.defaultDataScript = defaultDataScript;
    }

    public String getUrl() {
        //Using lock mode 0 to avoid lock timeouts from constantly rebuilding the database between tests
        //TODO remove when we can restart the database between tests instead of cleaning it
        return "jdbc:h2:mem:" + databaseName+ ";DB_CLOSE_DELAY=-1;LOCK_MODE=0";
    }

    public void runScriptPreInitialize(InputStream scriptStream) {
        JdbcDataSource jds = new JdbcDataSource();
        String url = getUrl();
        jds.setUrl(url);
        JdbcConnectionPool dataSource = JdbcConnectionPool.create(jds);

        dataSource.dispose();
    }

    @Override
    public void initialize() {
        JdbcDataSource jds = new JdbcDataSource();
        String url = getUrl();
        jds.setUrl(url);
        dataSource = JdbcConnectionPool.create(jds);
        transactionManager = new DataSourceTransactionManager(dataSource);

        DSLContext context = DSL.using(getConfig());

        DatabaseSchemaUpgrader upgrader = new DatabaseSchemaUpgrader(this,
                context,
                Common.getBean(ApplicationContext.class).getClassLoader());

        if(initWebConsole) {
            String webArgs[] = new String[4];
            webArgs[0] = "-webPort";
            webArgs[1] = webPort.toString();
            webArgs[2] = "-ifExists";
            webArgs[3] = "-webAllowOthers";
            try {
                this.web = Server.createWebServer(webArgs);
                this.web.start();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        ExtendedJdbcTemplate ejt = new ExtendedJdbcTemplate();
        ejt.setDataSource(getDataSource());

        if (altCreateScript != null) {
            try (InputStream in = altCreateScript.get()) {
                runScript(in, System.out);
            } catch (IOException e) {
                log.error("Failed to run altCreateScript", e);
            }
        }
        if (defaultDataScript != null) {
            try (InputStream in = defaultDataScript.get()) {
                runScript(in, System.out);
            } catch (IOException e) {
                log.error("Failed to run defaultDataScript", e);
            }
        }

        //Create the empty database
        if (!tableExists(ejt, Users.USERS.getName())) {
            // The users table wasn't found, so assume that this is a new instance.
            // Create the tables
            try (InputStream in = H2InMemoryDatabaseProxy.class.getResourceAsStream("createTables-" + getType().name() + ".sql")) {
                runScript(in, new NullOutputStream());
            } catch (IOException e) {
                log.error("createTables failed", e);
            }

            doInTransaction(txStatus -> {
                initializeCoreDatabase(context);
            });

            for (DatabaseSchemaDefinition def : ModuleRegistry.getDefinitions(DatabaseSchemaDefinition.class)) {
                try {
                    def.newInstallationCheck(ejt);
                } catch (Exception e) {
                    log.error("newInstallationCheck() failed", e);
                }
            }

            upgrader.setSystemSetting(SystemSettingsDao.DATABASE_SCHEMA_VERSION,
                    Integer.toString(Common.getDatabaseSchemaVersion()));
            upgrader.setSystemSetting(SystemSettingsDao.BACKUP_ENABLED, "false");
            upgrader.setSystemSetting(SystemSettingsDao.DATABASE_BACKUP_ENABLED, "false");

            // Add the settings flag that this is a new instance. This flag is removed when an administrator
            // logs in.
            upgrader.setSystemSetting(SystemSettingsDao.NEW_INSTANCE, BaseDao.boolToChar(true));
        }


        // The database exists, so let's make its schema version matches the application version.
        upgrader.checkCoreUpgrade();

        initialized = true;
    }

    @Override
    public DatabaseType getType() {
        return DatabaseType.H2;
    }

    @Override
    public void terminate() {
        if (dataSource != null)
            dataSource.dispose();
        if(web != null){
            if(web.isRunning(true)){
                web.stop();
                web.shutdown();
            }
        }
    }

    @Override
    public DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public boolean tableExists(ExtendedJdbcTemplate ejt, String tableName) {
        return ejt.queryForInt("SELECT COUNT(1) FROM INFORMATION_SCHEMA.TABLES WHERE table_name='"
                + tableName.toUpperCase() + "' AND table_schema='PUBLIC'", new Object[]{}, 0) > 0;
    }

    @Override
    public int getActiveConnections() {
        return dataSource.getActiveConnections();
    }

    @Override
    public int getIdleConnections() {
        return dataSource.getMaxConnections() - dataSource.getActiveConnections();
    }

    @Override
    public String getTableListQuery() {
        return "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE table_schema='PUBLIC'";
    }

    @Override
    public String getDatabasePassword(String propertyPrefix) {
        return null;
    }

    @Override
    public boolean isUseMetrics() {
        return this.useMetrics;
    }

    /**
     * Drop all data and reset to initial state
     * @throws Exception
     */
    public void clean() throws Exception {

        ExtendedJdbcTemplate ejt = new ExtendedJdbcTemplate();
        ejt.setDataSource(getDataSource());

        runScript(new String[]{"DROP ALL OBJECTS;"}, null);
        try (InputStream input = H2InMemoryDatabaseProxy.class.getResourceAsStream("createTables-" + getType().name() + ".sql")){
            runScript(input, null);
        }

        DSLContext context = DSL.using(getConfig());
        doInTransaction(txStatus -> {
            initializeCoreDatabase(context);
        });

        for (DatabaseSchemaDefinition def : ModuleRegistry.getDefinitions(DatabaseSchemaDefinition.class))
            def.newInstallationCheck(ejt);

        DatabaseSchemaUpgrader upgrader = new DatabaseSchemaUpgrader(this,
                context,
                Common.getBean(ApplicationContext.class).getClassLoader());

        upgrader.setSystemSetting(SystemSettingsDao.DATABASE_SCHEMA_VERSION,
                Integer.toString(Common.getDatabaseSchemaVersion()));

        // Add the settings flag that this is a new instance. This flag is removed when an administrator
        // logs in.
        upgrader.setSystemSetting(SystemSettingsDao.NEW_INSTANCE, BaseDao.boolToChar(true));

        //Clean the noSQL database
        Common.getBean(PointValueDao.class).deleteAllPointDataWithoutCount();

        //clear all caches in services
        Common.getRuntimeContext().getBeansOfType(CachingService.class).values().stream()
                .filter(s -> !(s instanceof PermissionService))
                .forEach(CachingService::clearCaches);

        // We clear the permission service cache afterwards as every call to clearCaches() on another service will
        // repopulate the role hierarchy cache
        Common.getRuntimeContext().getBean(PermissionService.class).clearCaches();
    }

    @Override
    public PlatformTransactionManager getTransactionManager() {
        return transactionManager;
    }

}
