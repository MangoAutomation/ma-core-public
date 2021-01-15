/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.db;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcConnectionPool;
import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.Server;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.PlatformTransactionManager;

import com.infiniteautomation.mango.spring.service.CachingService;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.util.NullOutputStream;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.DaoUtils;
import com.serotonin.db.spring.ConnectionCallbackVoid;
import com.serotonin.db.spring.ExtendedJdbcTemplate;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MockPointValueDao;
import com.serotonin.m2m2.db.dao.PointValueCacheDao;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.db.dao.PointValueDaoMetrics;
import com.serotonin.m2m2.db.dao.PointValueDaoSQL;
import com.serotonin.m2m2.db.dao.SchemaDefinition;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.db.upgrade.DBUpgrade;
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

    protected String databaseName = "test";
    protected JdbcConnectionPool dataSource;
    protected NoSQLProxy noSQLProxy;
    protected PointValueCacheProxy pointValueCacheProxy;
    protected MockPointValueDao mockPointValueDao;
    protected boolean initialized = false;
    protected boolean initWebConsole = false;
    protected Integer webPort;
    private Server web; //web UI
    protected DataSourceTransactionManager transactionManager;
    protected boolean useMetrics = false;
    protected Supplier<InputStream> altCreateScript = null;
    protected Supplier<InputStream> defaultDataScript = null;

    public H2InMemoryDatabaseProxy() {
        mockPointValueDao = new MockPointValueDao();
    }

    public H2InMemoryDatabaseProxy(boolean initWebConsole, Integer webPort) {
        this(initWebConsole, webPort, false);
        this.mockPointValueDao = new MockPointValueDao();
    }

    public H2InMemoryDatabaseProxy(boolean initWebConsole, Integer webPort, boolean useMetrics) {
        this(initWebConsole, webPort, useMetrics, null, null);
    }

    public H2InMemoryDatabaseProxy(boolean initWebConsole, Integer webPort, boolean useMetrics, Supplier<InputStream> altCreateScript, Supplier<InputStream> defaultDataScript) {
        this.mockPointValueDao = new MockPointValueDao();
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
    public void initialize(ClassLoader classLoader) {
        JdbcDataSource jds = new JdbcDataSource();
        String url = getUrl();
        jds.setUrl(url);
        dataSource = JdbcConnectionPool.create(jds);
        transactionManager = new DataSourceTransactionManager(dataSource);

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
            runScript(altCreateScript.get(), System.out);
        }
        if (defaultDataScript != null) {
            runScript(defaultDataScript.get(), System.out);
        }

        //Create the empty database
        if (!tableExists(ejt, SchemaDefinition.USERS_TABLE)) {
            // The users table wasn't found, so assume that this is a new instance.
            // Create the tables
            runScript(H2InMemoryDatabaseProxy.class.getResourceAsStream("createTables-" + getType().name() + ".sql"), System.out);

            for (DatabaseSchemaDefinition def : ModuleRegistry.getDefinitions(DatabaseSchemaDefinition.class))
                def.newInstallationCheck(ejt);

            SystemSettingsDao.instance.setValue(SystemSettingsDao.DATABASE_SCHEMA_VERSION,
                    Integer.toString(Common.getDatabaseSchemaVersion()));
            SystemSettingsDao.instance.setValue(SystemSettingsDao.BACKUP_ENABLED, "false");
            SystemSettingsDao.instance.setValue(SystemSettingsDao.DATABASE_BACKUP_ENABLED, "false");

            // Add the settings flag that this is a new instance. This flag is removed when an administrator
            // logs in.
            SystemSettingsDao.instance.setBooleanValue(SystemSettingsDao.NEW_INSTANCE, true);
        }

        // The database exists, so let's make its schema version matches the application version.
        DBUpgrade.checkUpgrade();

        // Check if we are using NoSQL
        NoSQLProxy proxy = ModuleRegistry.getDefinition(NoSQLProxy.class);
        if (proxy != null) {
            noSQLProxy = proxy;
            noSQLProxy.initialize();
        }

        initialized = true;
    }

    @Override
    public DatabaseType getType() {
        return DatabaseType.H2;
    }

    @Override
    public void terminate(boolean terminateNoSql) {
        if (dataSource != null)
            dataSource.dispose();
        if(web != null){
            if(web.isRunning(true)){
                web.stop();
                web.shutdown();
            }
        }
        // Check if we are using NoSQL
        if ((terminateNoSql)&&(noSQLProxy != null)) {
            noSQLProxy.shutdown();
        }
    }

    @Override
    public void terminateImpl() {


    }

    @Override
    public DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public double applyBounds(double value) {
        return value;
    }

    @Override
    public File getDataDirectory() {

        return null;
    }

    @Override
    public Long getDatabaseSizeInBytes() {
        return null;
    }

    @Override
    public void executeCompress(ExtendedJdbcTemplate ejt) {


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
    public void runScript(String[] script, OutputStream out) {
        ExtendedJdbcTemplate ejt = new ExtendedJdbcTemplate();
        ejt.setDataSource(getDataSource());

        StringBuilder statement = new StringBuilder();

        for (String line : script) {
            // Trim whitespace
            line = line.trim();

            // Skip comments
            if (line.startsWith("--"))
                continue;

            statement.append(line);
            statement.append(" ");
            if (line.endsWith(";")) {
                // Execute the statement
                ejt.execute(statement.toString());
                statement.delete(0, statement.length() - 1);
            }
        }
    }

    @Override
    public void runScript(InputStream input, OutputStream out) {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(input));

            List<String> lines = new ArrayList<>();
            String line;
            while ((line = in.readLine()) != null)
                lines.add(line);

            String[] script = new String[lines.size()];
            lines.toArray(script);
            runScript(script, out);
        }
        catch (Exception ioe) {
            throw new ShouldNeverHappenException(ioe);
        }
        finally {
            try {
                if (in != null)
                    in.close();
            }
            catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }
    }

    @Override
    public String getTableListQuery() {
        return "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE table_schema='PUBLIC'";
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
                throw new RuntimeException(e1);
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
    public <T> List<T> doLimitQuery(DaoUtils dao, String sql, Object[] args, RowMapper<T> rowMapper,
            int limit) {
        if (limit > 0)
            sql += " LIMIT " + limit;
        return dao.query(sql, args, rowMapper);
    }

    @Override
    public long doLimitDelete(ExtendedJdbcTemplate ejt, String sql, Object[] args, int chunkSize,
            int chunkWait, int limit) {
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

    protected String getLimitDelete(String sql, int chunkSize) {
        return sql + " LIMIT " + chunkSize;
    }

    @Override
    public String getDatabasePassword(String propertyPrefix) {
        return null;
    }

    @Override
    public void setNoSQLProxy(NoSQLProxy proxy) {
        this.noSQLProxy = proxy;
    }

    @Override
    public PointValueDao newPointValueDao() {
        if(initialized) {
            if (noSQLProxy == null) {
                if (useMetrics)
                    return new PointValueDaoMetrics(new PointValueDaoSQL());
                return new PointValueDaoSQL();
            }

            if (useMetrics)
                return noSQLProxy.createPointValueDaoMetrics();
            return noSQLProxy.createPointValueDao();
        }else
            return mockPointValueDao;
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
    public PointValueCacheDao getPointValueCacheDao() {
        return pointValueCacheProxy.getDao();
    }

    /**
     * Drop all data and reset to initial state
     * @throws Exception
     */
    public void clean() throws Exception {

        ExtendedJdbcTemplate ejt = new ExtendedJdbcTemplate();
        ejt.setDataSource(getDataSource());

        runScript(new String[]{"DROP ALL OBJECTS;"}, null);
        runScript(H2InMemoryDatabaseProxy.class.getResourceAsStream("createTables-" + getType().name() + ".sql"), null);

        for (DatabaseSchemaDefinition def : ModuleRegistry.getDefinitions(DatabaseSchemaDefinition.class))
            def.newInstallationCheck(ejt);

        SystemSettingsDao.instance.setValue(SystemSettingsDao.DATABASE_SCHEMA_VERSION,
                Integer.toString(Common.getDatabaseSchemaVersion()));

        // Add the settings flag that this is a new instance. This flag is removed when an administrator
        // logs in.
        SystemSettingsDao.instance.setBooleanValue(SystemSettingsDao.NEW_INSTANCE, true);

        //Clean the noSQL database
        if (noSQLProxy != null) {
            noSQLProxy.createPointValueDao().deleteAllPointDataWithoutCount();
        }

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

    @Override
    public OutputStream createLogOutputStream(Class<?> clazz) {
        return new NullOutputStream();
    }

}
