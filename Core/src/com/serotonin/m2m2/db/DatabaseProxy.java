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

import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.conf.RenderNameCase;
import org.jooq.conf.RenderQuotedNames;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.tools.StopWatchListener;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.PlatformTransactionManager;

import com.infiniteautomation.mango.db.tables.Roles;
import com.infiniteautomation.mango.db.tables.SystemSettings;
import com.infiniteautomation.mango.db.tables.UserRoleMappings;
import com.infiniteautomation.mango.db.tables.Users;
import com.serotonin.db.DaoUtils;
import com.serotonin.db.SpringConnectionProvider;
import com.serotonin.db.TransactionCapable;
import com.serotonin.db.spring.ConnectionCallbackVoid;
import com.serotonin.db.spring.ExtendedJdbcTemplate;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.BaseDao;
import com.serotonin.m2m2.db.dao.PointValueCacheDao;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.vo.permission.PermissionHolder;


/**
 *
 * @author Terry Packer
 */
public interface DatabaseProxy extends TransactionCapable {

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
    void setNoSQLProxy(NoSQLProxy proxy);

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
    PointValueCacheProxy getPointValueCacheProxy();

    /**
     * Get the point value cache dao
     * @return
     */
    PointValueCacheDao getPointValueCacheDao();

    boolean isUseMetrics();

    default Configuration getConfig() {
        Configuration configuration = new DefaultConfiguration();
        configuration.set(new SpringConnectionProvider(getDataSource()));

        boolean useMetrics = isUseMetrics();
        configuration.settings().setExecuteLogging(useMetrics);
        if (useMetrics) {
            configuration.set(StopWatchListener::new);
        }

        DatabaseType type = getType();
        configuration.set(type.getDialect());
        if (type == DatabaseType.H2) {
            configuration.settings()
                    .withRenderQuotedNames(RenderQuotedNames.EXPLICIT_DEFAULT_UNQUOTED)
                    .withRenderNameCase(RenderNameCase.AS_IS);
        }

        return configuration;
    }

    /**
     * Inserts and updates data for a new installation
     */
    default void initializeCoreDatabase(DSLContext context) {
        SystemSettings ss = SystemSettings.SYSTEM_SETTINGS;
        Roles r = Roles.ROLES;
        Users u = Users.USERS;
        UserRoleMappings urm = UserRoleMappings.USER_ROLE_MAPPINGS;

        context.insertInto(ss, ss.settingName, ss.settingValue)
                // Add the settings flag that this is a new instance. This flag is removed when an administrator logs in.
                .values(SystemSettingsDao.NEW_INSTANCE, BaseDao.boolToChar(true))
                // Record the current version.
                .values(SystemSettingsDao.DATABASE_SCHEMA_VERSION, Integer.toString(Common.getDatabaseSchemaVersion()))
                .execute();

        // TODO Mango 4.0 we should a setup page where on first login the admin chooses the locale, timezone and sets a new password

        context.insertInto(r, r.id, r.xid, r.name)
                .values(PermissionHolder.SUPERADMIN_ROLE.getId(), PermissionHolder.SUPERADMIN_ROLE.getXid(), Common.translate("roles.superadmin"))
                .values(PermissionHolder.USER_ROLE.getId(), PermissionHolder.USER_ROLE.getXid(), Common.translate("roles.user"))
                .values(PermissionHolder.ANONYMOUS_ROLE.getId(), PermissionHolder.ANONYMOUS_ROLE.getXid(), Common.translate("roles.anonymous"))
                .execute();

        if (Common.envProps.getBoolean("initialize.admin.create")) {
            long created = System.currentTimeMillis();

            int adminId = context.insertInto(u)
                    .set(u.name, Common.translate("users.defaultAdministratorName"))
                    .set(u.username, Common.envProps.getProperty("initialize.admin.username"))
                    .set(u.password, Common.encrypt(Common.envProps.getProperty("initialize.admin.password")))
                    .set(u.email, Common.envProps.getProperty("initialize.admin.email"))
                    .set(u.phone, "")
                    .set(u.disabled, BaseDao.boolToChar(false))
                    .set(u.lastLogin, 0L)
                    .set(u.homeUrl, "/ui/administration/home")
                    .set(u.receiveAlarmEmails, AlarmLevels.WARNING.value())
                    .set(u.receiveOwnAuditEvents, BaseDao.boolToChar(false))
                    .set(u.muted, BaseDao.boolToChar(true))
                    .set(u.tokenVersion, 1)
                    .set(u.passwordVersion, 1)
                    .set(u.passwordChangeTimestamp, created)
                    .set(u.sessionExpirationOverride, BaseDao.boolToChar(false))
                    .set(u.createdTs, created)
                    .returningResult(u.id)
                    .fetchOptional().orElseThrow(IllegalStateException::new)
                    .get(u.id);

            context.insertInto(urm, urm.userId, urm.roleId)
                    .values(adminId, PermissionHolder.SUPERADMIN_ROLE.getId())
                    .values(adminId, PermissionHolder.USER_ROLE.getId())
                    .execute();
        }
    }
}
