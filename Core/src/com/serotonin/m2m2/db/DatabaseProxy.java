/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
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
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.MissingResourceException;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DataSourceUtils;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.DaoUtils;
import com.serotonin.db.spring.ConnectionCallbackVoid;
import com.serotonin.db.spring.ExtendedJdbcTemplate;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.db.upgrade.DBUpgrade;
import com.serotonin.m2m2.module.DatabaseSchemaDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.DataPointAccess;

abstract public class DatabaseProxy {
    public enum DatabaseType {
        DERBY {
            @Override
            DatabaseProxy getImpl() {
                return new DerbyProxy();
            }
        },
        MSSQL {
            @Override
            DatabaseProxy getImpl() {
                return new MSSQLAccess();
            }
        },
        MYSQL {
            @Override
            DatabaseProxy getImpl() {
                return new MySQLProxy();
            }
        };

        abstract DatabaseProxy getImpl();
    }

    public static DatabaseProxy createDatabaseProxy() {
        String type = Common.envProps.getString("db.type", "derby");
        DatabaseType dt = DatabaseType.valueOf(type.toUpperCase());

        if (dt == null)
            throw new IllegalArgumentException("Unknown database type: " + type);

        return dt.getImpl();
    }

    private final Log log = LogFactory.getLog(DatabaseProxy.class);

    public void initialize(ClassLoader classLoader) {
        initializeImpl("");

        ExtendedJdbcTemplate ejt = new ExtendedJdbcTemplate();
        ejt.setDataSource(getDataSource());

        try {
            if (newDatabaseCheck(ejt)) {
                // Check if we should convert from another database.
                String convertTypeStr = null;
                try {
                    convertTypeStr = Common.envProps.getString("convert.db.type");
                }
                catch (MissingResourceException e) {
                    // no op
                }

                if (!StringUtils.isBlank(convertTypeStr)) {
                    // Found a database type from which to convert.
                    DatabaseType convertType = DatabaseType.valueOf(convertTypeStr.toUpperCase());
                    if (convertType == null)
                        throw new IllegalArgumentException("Unknown convert database type: " + convertType);

                    // TODO check that the convert source has the current DB version, or upgrade it if not.

                    DatabaseProxy sourceProxy = convertType.getImpl();
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

                    sourceProxy.terminate();
                }
                else {
                    // New database. Create a default user.
                    User user = new User();
                    user.setId(Common.NEW_ID);
                    user.setUsername("admin");
                    user.setPassword(Common.encrypt("admin"));
                    user.setEmail("admin@yourMangoDomain.com");
                    user.setPhone("");
                    user.setAdmin(true);
                    user.setDisabled(false);
                    user.setDataSourcePermissions(new LinkedList<Integer>());
                    user.setDataPointPermissions(new LinkedList<DataPointAccess>());
                    new UserDao().saveUser(user);

                    // Record the current version.
                    new SystemSettingsDao().setValue(SystemSettingsDao.DATABASE_SCHEMA_VERSION, Common.getVersion()
                            .getFullString());
                }
            }
            else
                // The database exists, so let's make its schema version matches the application version.
                DBUpgrade.checkUpgrade();
        }
        catch (CannotGetJdbcConnectionException e) {
            log.fatal("Unable to connect to database of type " + getType().name(), e);
            throw e;
        }

        // Allow modules to upgrade themselves
        for (DatabaseSchemaDefinition def : ModuleRegistry.getDefinitions(DatabaseSchemaDefinition.class))
            DBUpgrade.checkUpgrade(def, classLoader);

        postInitialize(ejt);
    }

    private boolean newDatabaseCheck(ExtendedJdbcTemplate ejt) {
        boolean coreIsNew = false;

        if (!tableExists(ejt, "users")) {
            // The users table wasn't found, so assume that this is a new instance.
            // Create the tables
            try {
                runScriptFile(Common.M2M2_HOME + "/db/createTables-" + getType().name() + ".sql", new FileOutputStream(
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

    abstract public DatabaseType getType();

    abstract public void terminate();

    abstract public DataSource getDataSource();

    abstract public double applyBounds(double value);

    abstract public File getDataDirectory();

    abstract public void executeCompress(ExtendedJdbcTemplate ejt);

    abstract protected void initializeImpl(String propertyPrefix);

    abstract public boolean tableExists(ExtendedJdbcTemplate ejt, String tableName);

    abstract public int getActiveConnections();

    abstract public int getIdleConnections();

    protected void postInitialize(@SuppressWarnings("unused") ExtendedJdbcTemplate ejt) {
        // no op - override as necessary
    }

    abstract public void runScript(String[] script, final OutputStream out) throws Exception;

    abstract public void runScript(InputStream in, final OutputStream out);

    abstract public String getTableListQuery();

    public void runScriptFile(String scriptFile, OutputStream out) {
        try {
            runScript(new FileInputStream(scriptFile), out);
        }
        catch (FileNotFoundException e) {
            throw new ShouldNeverHappenException(e);
        }
    }

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

    abstract public <T> List<T> doLimitQuery(DaoUtils dao, String sql, Object[] args, RowMapper<T> rowMapper, int limit);

    public String getDatabasePassword(String propertyPrefix) {
        String input = Common.envProps.getString(propertyPrefix + "db.password");
        return new DatabaseAccessUtils().decrypt(input);
    }
}
