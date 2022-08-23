/*
 * Copyright (C) 2022 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assume.assumeTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

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
    public void test1AutoIncrement() {
        DSLContext context = Common.getBean(DatabaseProxy.class).getContext();
        Roles r = Roles.ROLES;
        context.insertInto(r, r.id, r.xid, r.name).values(10, "xid", "name").execute();
        context.insertInto(r, r.xid, r.name).values("test", "test").execute();
        RoleVO role = Common.getBean(RoleDao.class).getByXid("test");
        assertNotEquals(11, role.getId());
        assertEquals(4, role.getId());

        context.execute("ALTER TABLE roles ALTER COLUMN id RESTART WITH 20");
        context.insertInto(r, r.xid, r.name).values("test2", "test2").execute();
        role = Common.getBean(RoleDao.class).getByXid("test2");
        assertEquals(20, role.getId());
    }

    // This test may break database
    // Needs to be executed as last test for the suite
    // https://github.com/h2database/h2database/issues/1808
    @Test(expected = TimeoutException.class)
    public void test2ExploitLobStorageMap() throws Throwable {
        Throwable ex = CompletableFuture.supplyAsync(()-> {
            try {
                return exploitLobStorageMap();
            } catch (Exception e) {
                return e;
            }
        })
        .orTimeout(10, TimeUnit.SECONDS)
        .handle((result, throwable) -> {
            if (throwable != null) return throwable;
            return result;
        }).get();

        throw ex;
    }

    public Exception exploitLobStorageMap() throws SQLException {
        AtomicReference<Exception> expected = new AtomicReference<>();

        Connection conn1 = Common.getBean(DatabaseProxy.class).getDataSource().getConnection();
        String createTable = "CREATE TABLE t1 (id int AUTO_INCREMENT, ver bigint, data text, PRIMARY KEY (id))";
        conn1.prepareStatement(createTable).executeUpdate();

        String insert = "INSERT INTO t1 (id, ver, data) values (1, 0, ?)";
        PreparedStatement insertStmt = conn1.prepareStatement(insert);
        String largeData = org.h2.util.StringUtils.pad("", 512, "x", false);
        insertStmt.setString(1, largeData);
        insertStmt.executeUpdate();

        Connection conn2 = Common.getBean(DatabaseProxy.class).getDataSource().getConnection();
        Thread updateTask = new Thread(() -> {
            try {
                String update = "UPDATE t1 SET ver = ver + 1 WHERE id = 1";
                while (true) {
                    conn2.prepareStatement(update).executeUpdate();
                }
            } catch (Exception ex) { expected.set(ex); }
        });

        updateTask.start();
        try {
            while (true) {
                conn1.prepareStatement("SELECT * FROM t1").executeQuery();
            }
        } catch (Exception ex) { expected.set(ex); }
        updateTask.interrupt();

        return expected.get();
    }
}
