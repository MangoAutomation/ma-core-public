/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;

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
        public DatabaseProxy databaseProxy(DatabaseProxyConfiguration configuration) {
            return new H2Proxy(null, configuration) {
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
