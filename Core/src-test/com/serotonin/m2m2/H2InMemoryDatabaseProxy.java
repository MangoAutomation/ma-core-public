/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcConnectionPool;
import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.Server;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.PlatformTransactionManager;

import com.infiniteautomation.mango.monitor.IntegerMonitor;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.DaoUtils;
import com.serotonin.db.spring.ConnectionCallbackVoid;
import com.serotonin.db.spring.ExtendedJdbcTemplate;
import com.serotonin.m2m2.db.DatabaseProxy;
import com.serotonin.m2m2.db.NoSQLProxy;
import com.serotonin.m2m2.db.NoSQLProxyFactory;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.db.dao.PointValueDaoMetrics;
import com.serotonin.m2m2.db.dao.PointValueDaoSQL;
import com.serotonin.m2m2.db.dao.SchemaDefinition;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.db.upgrade.DBUpgrade;
import com.serotonin.m2m2.module.DatabaseSchemaDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.definitions.permissions.SuperadminPermissionDefinition;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.template.DefaultDataPointPropertiesTemplateFactory;
import com.serotonin.provider.Providers;

/**
 * Using an H2 in memory database we can easily mock the database proxy.
 * 
 * If you do not call initialize, none of the tables are created.  However 
 *   it is possible to use the PointValueDao via the mock in memory list implementation.
 *  
 * @author Terry Packer
 */
public class H2InMemoryDatabaseProxy implements DatabaseProxy{
    
    protected String databaseName = "test";
    protected JdbcConnectionPool dataSource;
    protected NoSQLProxy noSQLProxy;
    protected MockPointValueDao mockPointValueDao;
    protected boolean initialized = false;
    protected boolean initWebConsole = false;
    protected Integer webPort;
    private Server web; //web UI
    protected DataSourceTransactionManager transactionManager;
    protected boolean useMetrics = false;
    protected String altCreateScript = null;
    protected String defaultDataScript = null;
    
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
    
    public H2InMemoryDatabaseProxy(boolean initWebConsole, Integer webPort, boolean useMetrics, String altCreateScript, String defaultDataScript) {
        this.mockPointValueDao = new MockPointValueDao();
        this.initWebConsole = initWebConsole;
        this.webPort = webPort;
        this.useMetrics = useMetrics;
        this.altCreateScript = altCreateScript;
        this.defaultDataScript = defaultDataScript;
    }
    
    public String getUrl() {
        return "jdbc:h2:mem:" + databaseName+ ";DB_CLOSE_DELAY=-1";
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
        
        if(altCreateScript != null)
            runScript(this.getClass().getResourceAsStream(altCreateScript), System.out);
        if(defaultDataScript != null)
            runScript(this.getClass().getResourceAsStream(defaultDataScript), System.out);
        
        //Create the empty database
        if (!tableExists(ejt, SchemaDefinition.USERS_TABLE)) {
            // The users table wasn't found, so assume that this is a new instance.
            // Create the tables
            runScript(this.getClass().getResourceAsStream("/createTables-" + getType().name() + ".sql"), System.out);

            for (DatabaseSchemaDefinition def : ModuleRegistry.getDefinitions(DatabaseSchemaDefinition.class))
                def.newInstallationCheck(ejt);
            
            SystemSettingsDao.instance.setValue(SystemSettingsDao.DATABASE_SCHEMA_VERSION,
                    Integer.toString(Common.getDatabaseSchemaVersion()));
            SystemSettingsDao.instance.setValue(SystemSettingsDao.BACKUP_ENABLED, "false");
            SystemSettingsDao.instance.setValue(SystemSettingsDao.DATABASE_BACKUP_ENABLED, "false");
            
            // Add the settings flag that this is a new instance. This flag is removed when an administrator
            // logs in.
            SystemSettingsDao.instance.setBooleanValue(SystemSettingsDao.NEW_INSTANCE, true);
            
            Providers.get(IMangoLifecycle.class).addStartupTask(new Runnable() {
                @Override
                public void run() {
                        // New database. Create a default user.
                    User user = new User();
                    user.setId(Common.NEW_ID);
                    user.setName("Administrator");
                    user.setUsername("admin");
                    user.setPassword(Common.encrypt("admin"));
                    user.setEmail("admin@yourMangoDomain.com");
                    user.setPhone("");
                    user.setPermissions(SuperadminPermissionDefinition.GROUP_NAME);
                    user.setDisabled(false);
                    UserDao.getInstance().saveUser(user);
                    
                    DefaultDataPointPropertiesTemplateFactory factory = new DefaultDataPointPropertiesTemplateFactory();
                    factory.saveDefaultTemplates();
               }
            });
        } 
        
        // The database exists, so let's make its schema version matches the application version.
        DBUpgrade.checkUpgrade();
        
        // Check if we are using NoSQL
        if (NoSQLProxyFactory.instance.getProxy() != null) {
            noSQLProxy = NoSQLProxyFactory.instance.getProxy();
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
        if ((terminateNoSql)&&(NoSQLProxyFactory.instance.getProxy() != null)) {
            noSQLProxy = NoSQLProxyFactory.instance.getProxy();
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
        return ejt.queryForObject("SELECT COUNT(1) FROM INFORMATION_SCHEMA.TABLES WHERE table_name='"
                + tableName.toUpperCase() + "' AND table_schema='PUBLIC'", new Object[]{}, Integer.class, 0) > 0;
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
    public void runScript(String[] script, OutputStream out) throws Exception {
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
    public void runScriptFile(String scriptFile, OutputStream out) {
        try {
            runScript(new FileInputStream(scriptFile), out);
        }
        catch (FileNotFoundException e) {
            throw new ShouldNeverHappenException(e);
        }
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
            sql = getLimitQuerySql(sql, limit);
        return dao.query(sql, args, rowMapper);
    }
    
    @Override
    public String getLimitQuerySql(String sql, int limit) {
        if (limit > 0)
            return sql + " LIMIT " + limit;
        return sql;
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

    /**
     * Drop all data and reset to initial state
     * @throws Exception
     */
    public void clean() throws Exception {
        
        //If the SQL Batch writer is executing we need to allow it to finish 
        if(initialized && noSQLProxy == null) {
            IntegerMonitor m = (IntegerMonitor)Common.MONITORED_VALUES.getValueMonitor(PointValueDaoSQL.INSTANCES_MONITOR_ID);
            if(m != null) {
                int retries = 100;
                while(retries > 0) {
                    if(m.getValue() == 0)
                        break;
                    retries--;
                    Thread.sleep(100);
                }
                if(retries == 0)
                    fail("SQL Batch write behind still running.");
            }
        }
        
        ExtendedJdbcTemplate ejt = new ExtendedJdbcTemplate();
        ejt.setDataSource(getDataSource());
        
        //Drop everything
        runScript(new String[] {"DROP ALL OBJECTS;"}, null);
        //Create database
        runScript(this.getClass().getResourceAsStream("/createTables-" + getType().name() + ".sql"), null);

        for (DatabaseSchemaDefinition def : ModuleRegistry.getDefinitions(DatabaseSchemaDefinition.class))
            def.newInstallationCheck(ejt);

        SystemSettingsDao.instance.setValue(SystemSettingsDao.DATABASE_SCHEMA_VERSION,
                Integer.toString(Common.getDatabaseSchemaVersion()));

        // Add the settings flag that this is a new instance. This flag is removed when an administrator
        // logs in.
        SystemSettingsDao.instance.setBooleanValue(SystemSettingsDao.NEW_INSTANCE, true);
        
        User user = new User();
        user.setId(Common.NEW_ID);
        user.setName("Administrator");
        user.setUsername("admin");
        user.setPassword(Common.encrypt("admin"));
        user.setEmail("admin@yourMangoDomain.com");
        user.setPhone("");
        user.setPermissions(SuperadminPermissionDefinition.GROUP_NAME);
        user.setDisabled(false);
        UserDao.getInstance().saveUser(user);
        
        //Clean the noSQL database
        // Check if we are using NoSQL
        if (NoSQLProxyFactory.instance.getProxy() != null) {
            noSQLProxy = NoSQLProxyFactory.instance.getProxy();
            noSQLProxy.createPointValueDao().deleteAllPointDataWithoutCount();
        }
    }

    @Override
    public PlatformTransactionManager getTransactionManager() {
        return transactionManager;
    }
    
}
