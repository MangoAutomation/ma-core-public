/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.h2.jdbc.JdbcSQLNonTransientException;
import org.h2.jdbc.JdbcSQLSyntaxErrorException;
import org.jooq.DSLContext;
import org.junit.Test;
import org.springframework.context.annotation.Bean;

import com.infiniteautomation.mango.db.tables.Roles;
import com.infiniteautomation.mango.spring.DatabaseProxyConfiguration;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.MockMangoLifecycle;
import com.serotonin.m2m2.db.dao.RoleDao;
import com.serotonin.m2m2.vo.role.RoleVO;

/**
 * Perform a full upgrade from version 1 of the schema on an H2 database.
 *
 * @author Terry Packer
 */
public class H2DatabaseUpgradeTest extends MangoTestBase {

    public H2DatabaseUpgradeTest() {
        String currentDb = properties.getProperty("db.type");
        assumeTrue(currentDb.equals("h2"));
    }

    @Test
    public void doUpgrade() {
        // do nothing, before hook will throw exception if upgrade fails
    }

    // https://github.com/h2database/h2database/issues/1808
    @Test
    public void exploitLobStorageMap() throws SQLException {
        Exception expected = null;
        try {
            Connection conn1 = Common.getBean(DatabaseProxy.class).getDataSource().getConnection();
            String createTable = "CREATE TABLE t1 (id int AUTO_INCREMENT, ver bigint, data text, PRIMARY KEY (id))";
            conn1.prepareStatement(createTable).executeUpdate();

            String insert = "INSERT INTO t1 (id, ver, data) values (1, 0, ?)";
            PreparedStatement insertStmt = conn1.prepareStatement(insert);
            String largeData = org.h2.util.StringUtils.pad("", 512, "x", false);
            insertStmt.setString(1, largeData);
            insertStmt.executeUpdate();

            new Thread(() -> {
                try {
                    Connection conn2 = Common.getBean(DatabaseProxy.class).getDataSource().getConnection();
                    String update = "UPDATE t1 SET ver = ver + 1 WHERE id = 1";
                    while (!Thread.currentThread().isInterrupted()) {
                        conn2.prepareStatement(update).executeUpdate();
                    }
                } catch (JdbcSQLSyntaxErrorException ex) {
                    assertEquals("42S02", ex.getSQLState());
                    assertEquals("UPDATE t1 SET ver = ver + 1 WHERE id = 1", ex.getSQL());
                    assertEquals("Table \"T1\" not found", ex.getOriginalMessage());
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            }).start();

            while (true) {
                conn1.prepareStatement("SELECT * FROM t1").executeQuery();
            }
        } catch (JdbcSQLNonTransientException ex) {
            expected = ex;
            assertEquals("HY000", ex.getSQLState());
            assertEquals("SELECT * FROM t1", ex.getSQL());
            assertEquals("General error: \"java.lang.NullPointerException\"", ex.getOriginalMessage());
        }
        assertNotNull(expected);
    }

    @Test
    public void testAutoIncrement() throws SQLException {
        DSLContext context = Common.getBean(DatabaseProxy.class).getContext();
        Roles r = Roles.ROLES;
        context.insertInto(r, r.id, r.xid, r.name).values(10, "xid", "name").execute();
        context.insertInto(r, r.xid, r.name).values("test", "test").execute();
        RoleVO role = Common.getBean(RoleDao.class).getByXid("test");
        assertEquals(11, role.getId());
    }

    public static class UpgradeConfig {
        @Bean
        public DatabaseProxy databaseProxy(DatabaseProxyConfiguration configuration, String propertyPrefix) {
            return new H2Proxy(null, configuration, propertyPrefix) {
                @Override
                protected boolean restoreTables() throws IOException {
                    try (var outputStream = createTablesOutputStream()) {
                        try (var inputStream = getClass().getResourceAsStream("version1/createTables-H2.sql")) {
                            runScript(inputStream, outputStream);
                        }
                        try (var inputStream = getClass().getResourceAsStream("version1/defaultData-H2.sql")) {
                            runScript(inputStream, outputStream);
                        }
                    }
                    return true;
                }
            };
        }
    }

    @Override
    protected MockMangoLifecycle getLifecycle() {
        MockMangoLifecycle lifecycle = super.getLifecycle();
        lifecycle.addRuntimeContextConfiguration(UpgradeConfig.class);
        return lifecycle;
    }
}
