/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.sql.SQLException;

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
 * Perform a full upgrade from version 1 of the schema on an POSTGRES database.
 *
 * @author Terry Packer
 */
public class PostgresDatabaseUpgradeTest extends MangoTestBase {

    public PostgresDatabaseUpgradeTest() {
        String currentDb = properties.getProperty("db.type");
        assumeTrue(currentDb.equals("postgres"));
    }

    @Test
    public void doUpgrade() {
        // do nothing, before hook will throw exception if upgrade fails
    }

    @Test
    public void testAutoIncrement() throws SQLException {
        DSLContext context = Common.getBean(DatabaseProxy.class).getContext();
        Roles r = Roles.ROLES;
        context.insertInto(r, r.id, r.xid, r.name).values(10, "xid", "name").execute();
        context.insertInto(r, r.xid, r.name).values("test", "test").execute();
        RoleVO role = Common.getBean(RoleDao.class).getByXid("test");
        assertNotEquals(11, role.getId());
        assertEquals(4, role.getId());

        String sequence = r.getName() + "_" + r.id.getName() + "_seq";
        context.alterSequence(sequence).restartWith(20).execute();
        context.insertInto(r, r.xid, r.name).values("test2", "test2").execute();
        role = Common.getBean(RoleDao.class).getByXid("test2");
        assertEquals(20, role.getId());
    }

    public static class UpgradeConfig {
        @Bean
        public DatabaseProxy databaseProxy(DatabaseProxyConfiguration configuration) {
            return new PostgresProxy(null, configuration) {
                @Override
                protected boolean restoreTables() throws IOException {
                    try (var outputStream = createTablesOutputStream()) {
                        try (var inputStream = getClass().getResourceAsStream("version1/createTables-POSTGRES.sql")) {
                            runScript(inputStream, outputStream);
                        }
                        try (var inputStream = getClass().getResourceAsStream("version1/defaultData-POSTGRES.sql")) {
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
