/*
 * Copyright (C) 2022 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.h2.jdbc.JdbcSQLNonTransientException;
import org.h2.jdbc.JdbcSQLSyntaxErrorException;
import org.jooq.DSLContext;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.infiniteautomation.mango.db.tables.Roles;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.db.dao.RoleDao;
import com.serotonin.m2m2.vo.role.RoleVO;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class H2DatabaseTest extends MangoTestBase {

    public H2DatabaseTest() {
        String currentDb = properties.getProperty("db.type");
        assumeTrue(currentDb.equals("h2"));
    }

    @Test
    public void test1AutoIncrement() throws SQLException {
        DSLContext context = Common.getBean(DatabaseProxy.class).getContext();
        Roles r = Roles.ROLES;
        context.insertInto(r, r.id, r.xid, r.name).values(10, "xid", "name").execute();
        context.insertInto(r, r.xid, r.name).values("test", "test").execute();
        RoleVO role = Common.getBean(RoleDao.class).getByXid("test");
        assertEquals(11, role.getId());
    }

    // This test may break database
    // Needs to be executed as last test for the suite
    // https://github.com/h2database/h2database/issues/1808
    @Test(timeout = 10 * 1000)
    public void test2ExploitLobStorageMap() throws SQLException {
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

            var jvm11Error = "General error: \"java.lang.NullPointerException\"";
            var jvm17_18Error = "General error: \"java.lang.NullPointerException: Cannot invoke \"\"[Ljava.lang.Object;.clone()\"\" because \"\"<local10>\"\" is null\"";

            var rt = ManagementFactory.getRuntimeMXBean();
            if (rt.getVmVersion().startsWith("11")) {
                assertEquals(jvm11Error, ex.getOriginalMessage());
            } else {
                assertEquals(jvm17_18Error, ex.getOriginalMessage());
            }
        }
        assertNotNull(expected);
    }
}
