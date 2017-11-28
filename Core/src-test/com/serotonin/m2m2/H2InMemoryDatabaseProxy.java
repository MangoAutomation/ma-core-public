/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2;

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
    
    public H2InMemoryDatabaseProxy() {
        mockPointValueDao = new MockPointValueDao();
    }
    
    public H2InMemoryDatabaseProxy(boolean initWebConsole, Integer webPort) {
        this(initWebConsole, webPort, false);
        this.mockPointValueDao = new MockPointValueDao();
    }
    
    public H2InMemoryDatabaseProxy(boolean initWebConsole, Integer webPort, boolean useMetrics) {
        this.mockPointValueDao = new MockPointValueDao();
        this.initWebConsole = initWebConsole;
        this.webPort = webPort;
        this.useMetrics = useMetrics;
    }
    
    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.DatabaseProxy#initialize(java.lang.ClassLoader)
     */
    @Override
    public void initialize(ClassLoader classLoader) {
        JdbcDataSource jds = new JdbcDataSource();
        String url = "jdbc:h2:mem:" + databaseName + ";DB_CLOSE_DELAY=-1";
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
        
        //Create the empty database
        if (!tableExists(ejt, SchemaDefinition.USERS_TABLE)) {
            // The users table wasn't found, so assume that this is a new instance.
            // Create the tables
            runScript(this.getClass().getResourceAsStream("/createTables-" + getType().name() + ".sql"), null);

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
                    UserDao.instance.saveUser(user);
                    
                    DefaultDataPointPropertiesTemplateFactory factory = new DefaultDataPointPropertiesTemplateFactory();
                    factory.saveDefaultTemplates();
               }
            });
        } 
        
        // Check if we are using NoSQL
        if (NoSQLProxyFactory.instance.getProxy() != null) {
            noSQLProxy = NoSQLProxyFactory.instance.getProxy();
            noSQLProxy.initialize();
        }
        
        initialized = true;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.DatabaseProxy#getType()
     */
    @Override
    public DatabaseType getType() {
       return DatabaseType.H2;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.DatabaseProxy#terminate(boolean)
     */
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

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.DatabaseProxy#terminateImpl()
     */
    @Override
    public void terminateImpl() {

        
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.DatabaseProxy#getDataSource()
     */
    @Override
    public DataSource getDataSource() {
        return dataSource;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.DatabaseProxy#applyBounds(double)
     */
    @Override
    public double applyBounds(double value) {
        return value;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.DatabaseProxy#getDataDirectory()
     */
    @Override
    public File getDataDirectory() {

        return null;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.DatabaseProxy#getDatabaseSizeInBytes()
     */
    @Override
    public Long getDatabaseSizeInBytes() {
        return null;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.DatabaseProxy#executeCompress(com.serotonin.db.spring.ExtendedJdbcTemplate)
     */
    @Override
    public void executeCompress(ExtendedJdbcTemplate ejt) {

        
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.DatabaseProxy#tableExists(com.serotonin.db.spring.ExtendedJdbcTemplate, java.lang.String)
     */
    @Override
    public boolean tableExists(ExtendedJdbcTemplate ejt, String tableName) {
        return ejt.queryForObject("SELECT COUNT(1) FROM INFORMATION_SCHEMA.TABLES WHERE table_name='"
                + tableName.toUpperCase() + "' AND table_schema='PUBLIC'", new Object[]{}, Integer.class, 0) > 0;
    }
    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.DatabaseProxy#getActiveConnections()
     */
    @Override
    public int getActiveConnections() {
        return dataSource.getActiveConnections();
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.DatabaseProxy#getIdleConnections()
     */
    @Override
    public int getIdleConnections() {
        return dataSource.getMaxConnections() - dataSource.getActiveConnections();
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.DatabaseProxy#runScript(java.lang.String[], java.io.OutputStream)
     */
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

            // Replace macros in the line
            //            line = com.serotonin.util.StringUtils.replaceMacro(line, "blob", replacement("blob"));
            //            line = com.serotonin.util.StringUtils.replaceMacro(line, "char", replacement("char"));
            //            line = com.serotonin.util.StringUtils.replaceMacro(line, "clob", replacement("clob"));
            //            line = com.serotonin.util.StringUtils.replaceMacro(line, "double", replacement("double"));
            //            line = com.serotonin.util.StringUtils.replaceMacro(line, "identity", replacement("identity"));
            //            line = com.serotonin.util.StringUtils.replaceMacro(line, "int", replacement("int"));
            //            line = com.serotonin.util.StringUtils.replaceMacro(line, "varchar", replacement("varchar"));
            //
            //            line = com.serotonin.util.StringUtils.replaceMacro(line, "ALTER COLUMN", replacement("ALTER COLUMN"));
            //            line = com.serotonin.util.StringUtils.replaceMacro(line, "DROP FOREIGN KEY",
            //                    replacement("DROP FOREIGN KEY"));

            statement.append(line);
            statement.append(" ");
            if (line.endsWith(";")) {
                // Execute the statement
                ejt.execute(statement.toString());
                statement.delete(0, statement.length() - 1);
            }
        }
        
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.DatabaseProxy#runScript(java.io.InputStream, java.io.OutputStream)
     */
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

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.DatabaseProxy#getTableListQuery()
     */
    @Override
    public String getTableListQuery() {
        return "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE table_schema='PUBLIC'";
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.DatabaseProxy#runScriptFile(java.lang.String, java.io.OutputStream)
     */
    @Override
    public void runScriptFile(String scriptFile, OutputStream out) {
        try {
            runScript(new FileInputStream(scriptFile), out);
        }
        catch (FileNotFoundException e) {
            throw new ShouldNeverHappenException(e);
        }
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.DatabaseProxy#doInConnection(com.serotonin.db.spring.ConnectionCallbackVoid)
     */
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

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.DatabaseProxy#doLimitQuery(com.serotonin.db.DaoUtils, java.lang.String, java.lang.Object[], org.springframework.jdbc.core.RowMapper, int)
     */
    @Override
    public <T> List<T> doLimitQuery(DaoUtils dao, String sql, Object[] args, RowMapper<T> rowMapper,
            int limit) {
        if (limit > 0)
            sql += " LIMIT " + limit;
        return dao.query(sql, args, rowMapper);
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.DatabaseProxy#doLimitDelete(com.serotonin.db.spring.ExtendedJdbcTemplate, java.lang.String, java.lang.Object[], int, int, int)
     */
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
    
    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.DatabaseProxy#getDatabasePassword(java.lang.String)
     */
    @Override
    public String getDatabasePassword(String propertyPrefix) {
        return null;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.DatabaseProxy#setNoSQLProxy(com.serotonin.m2m2.db.NoSQLProxy)
     */
    @Override
    public void setNoSQLProxy(NoSQLProxy proxy) {
        this.noSQLProxy = proxy;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.DatabaseProxy#newPointValueDao()
     */
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

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.DatabaseProxy#getNoSQLProxy()
     */
    @Override
    public NoSQLProxy getNoSQLProxy() {
        return noSQLProxy;
    }

    /**
     * @throws Exception 
     * 
     */
    public void clean() throws Exception {
        
        ExtendedJdbcTemplate ejt = new ExtendedJdbcTemplate();
        ejt.setDataSource(getDataSource());
        
        runScript(new String[] {"DROP ALL OBJECTS;"}, null);
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
        UserDao.instance.saveUser(user);
    }

    @Override
    public PlatformTransactionManager getTransactionManager() {
        return transactionManager;
    }
    
}
