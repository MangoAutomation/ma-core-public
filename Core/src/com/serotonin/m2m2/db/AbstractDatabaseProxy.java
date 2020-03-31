/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.MissingResourceException;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.PlatformTransactionManager;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.DaoUtils;
import com.serotonin.db.spring.ConnectionCallbackVoid;
import com.serotonin.db.spring.ExtendedJdbcTemplate;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.Constants;
import com.serotonin.m2m2.IMangoLifecycle;
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

abstract public class AbstractDatabaseProxy implements DatabaseProxy {

    public static DatabaseProxy createDatabaseProxy() {
        String type = Common.envProps.getString("db.type", "h2");
        DatabaseType dt = DatabaseType.valueOf(type.toUpperCase());

        if (dt == null)
            throw new IllegalArgumentException("Unknown database type: " + type);

        return dt.getImpl();
    }

    private final Log log = LogFactory.getLog(AbstractDatabaseProxy.class);
    private NoSQLProxy noSQLProxy;
    private Boolean useMetrics;
    private PlatformTransactionManager transactionManager;

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.DatabaseProxy#initialize(java.lang.ClassLoader)
     */
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
                runScript(new String[] {"CREATE TABLE mangoUpgrade28 (test JSON)enginInnoDB;", "DROP TABLE mangoUpgrade28;"}, null);
            }catch(Exception e) {
                String version = "unkown";
                try {
                    DatabaseMetaData dmd = getDataSource().getConnection().getMetaData();
                    version = dmd.getDatabaseProductVersion();
                }catch(Exception ex) {
                    //Munch
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
                    if (convertType == null)
                        throw new IllegalArgumentException("Unknown convert database type: " + convertType);

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

                    /**
                     * Add a startup task to run after the Audit system is ready
                     */
                    Providers.get(IMangoLifecycle.class).addStartupTask(new Runnable() {
                        @Override
                        public void run() {
                            // New database. Create a default user.
                            User user = new User();
                            user.setId(Common.NEW_ID);
                            user.setName("Administrator");
                            user.setUsername("admin");
                            user.setPassword(Common.encrypt("admin"));
                            user.setEmail("admin@mango.example.com");
                            user.setPhone("");
                            user.setPermissions(SuperadminPermissionDefinition.GROUP_NAME);
                            user.setDisabled(false);
                            user.setHomeUrl("/ui/administration/home");
                            UserDao.getInstance().saveUser(user);

                            DefaultDataPointPropertiesTemplateFactory factory = new DefaultDataPointPropertiesTemplateFactory();
                            factory.saveDefaultTemplates();
                        }
                    });
                }
            }
            else
                // The database exists, so let's make its schema version matches the application version.
                DBUpgrade.checkUpgrade();

            // Check if we are using NoSQL
            if (NoSQLProxyFactory.instance.getProxy() != null) {
                noSQLProxy = NoSQLProxyFactory.instance.getProxy();
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

        // Allow modules to upgrade themselves
        for (DatabaseSchemaDefinition def : ModuleRegistry.getDefinitions(DatabaseSchemaDefinition.class))
            DBUpgrade.checkUpgrade(def, classLoader);

        postInitialize(ejt, "", newDatabase);
    }

    private boolean newDatabaseCheck(ExtendedJdbcTemplate ejt) {
        boolean coreIsNew = false;

        if (!tableExists(ejt, SchemaDefinition.USERS_TABLE)) {
            // The users table wasn't found, so assume that this is a new instance.
            // Create the tables
            try {
                File installScript = Common.MA_HOME_PATH
                        .resolve(Constants.DIR_DB)
                        .resolve("createTables-" + getType().name() + ".sql")
                        .toFile();
                runScriptFile(installScript, new FileOutputStream(
                        new File(Common.getLogsDir(), "createTables.log")));
            }
            catch (FileNotFoundException e) {
                throw new ShouldNeverHappenException(e);
            }
            coreIsNew = true;
        }

        for (DatabaseSchemaDefinition def : ModuleRegistry.getDefinitions(DatabaseSchemaDefinition.class))
            def.newInstallationCheck(ejt);

        return coreIsNew;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.DatabaseProxy#getType()
     */
    @Override
    abstract public DatabaseType getType();

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.DatabaseProxy#terminate(boolean)
     */
    @Override
    public void terminate(boolean terminateNoSql) {
        terminateImpl();
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
    abstract public void terminateImpl();

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.DatabaseProxy#getDataSource()
     */
    @Override
    abstract public DataSource getDataSource();

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.DatabaseProxy#applyBounds(double)
     */
    @Override
    abstract public double applyBounds(double value);

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.DatabaseProxy#getDataDirectory()
     */
    @Override
    abstract public File getDataDirectory();

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.DatabaseProxy#getDatabaseSizeInBytes()
     */
    @Override
    abstract public Long getDatabaseSizeInBytes();

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.DatabaseProxy#executeCompress(com.serotonin.db.spring.ExtendedJdbcTemplate)
     */
    @Override
    abstract public void executeCompress(ExtendedJdbcTemplate ejt);

    abstract protected void initializeImpl(String propertyPrefix);

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.DatabaseProxy#tableExists(com.serotonin.db.spring.ExtendedJdbcTemplate, java.lang.String)
     */
    @Override
    abstract public boolean tableExists(ExtendedJdbcTemplate ejt, String tableName);

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.DatabaseProxy#getActiveConnections()
     */
    @Override
    abstract public int getActiveConnections();

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.DatabaseProxy#getIdleConnections()
     */
    @Override
    abstract public int getIdleConnections();

    protected void postInitialize(ExtendedJdbcTemplate ejt, String propertyPrefix, boolean newDatabase) {
        // no op - override as necessary
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.DatabaseProxy#runScript(java.lang.String[], java.io.OutputStream)
     */
    @Override
    abstract public void runScript(String[] script, final OutputStream out) throws Exception;

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.DatabaseProxy#runScript(java.io.InputStream, java.io.OutputStream)
     */
    @Override
    abstract public void runScript(InputStream in, final OutputStream out);

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.DatabaseProxy#getTableListQuery()
     */
    @Override
    abstract public String getTableListQuery();

    @Override
    public void runScriptFile(File scriptFile, OutputStream out) {
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

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.DatabaseProxy#doLimitQuery(com.serotonin.db.DaoUtils, java.lang.String, java.lang.Object[], org.springframework.jdbc.core.RowMapper, int)
     */
    @Override
    abstract public <T> List<T> doLimitQuery(DaoUtils dao, String sql, Object[] args, RowMapper<T> rowMapper, int limit);

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.DatabaseProxy#doLimitDelete(com.serotonin.db.spring.ExtendedJdbcTemplate, java.lang.String, java.lang.Object[], int, int, int)
     */
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

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.DatabaseProxy#getDatabasePassword(java.lang.String)
     */
    @Override
    public String getDatabasePassword(String propertyPrefix) {
        String input = Common.envProps.getString(propertyPrefix + "db.password");
        return new DatabaseAccessUtils().decrypt(input);
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
        if (noSQLProxy == null) {
            if (useMetrics)
                return new PointValueDaoMetrics(new PointValueDaoSQL());
            return new PointValueDaoSQL();
        }

        if (useMetrics)
            return noSQLProxy.createPointValueDaoMetrics();
        return noSQLProxy.createPointValueDao();
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.DatabaseProxy#getNoSQLProxy()
     */
    @Override
    public NoSQLProxy getNoSQLProxy() {
        return noSQLProxy;
    }

    @Override
    public PlatformTransactionManager getTransactionManager() {
        return transactionManager;
    }

    //  TODO: could potentially expose Logging DAO for use in application
    //  	currently not implemented except for IasTsdb
    //	/**
    //	 * Get an instance of the Logging Dao
    //	 *
    //	 * @return
    //	 */
    //	public LoggingDao newLoggingDao() {
    //        if (noSQLProxy == null){
    //        	if(useMetrics)
    //        		return new LoggingDaoMetrics(new LoggingDaoSQL());
    //        	else
    //        		return new LoggingDaoSQL();
    //        }else{
    //        	if(useMetrics)
    //        		return new LoggingDaoMetrics(noSQLProxy.createLoggingDao());
    //        	else
    //        		return noSQLProxy.createLoggingDao();
    //        }
    //	}

}
