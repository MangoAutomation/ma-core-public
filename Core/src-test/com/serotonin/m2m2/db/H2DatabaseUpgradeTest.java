/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db;

import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.infiniteautomation.mango.spring.DatabaseProxyConfiguration;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.MockMangoLifecycle;

/**
 * Perform a full upgrade from version 1 of the schema on an H2 database.
 *
 * @author Terry Packer
 */
public class H2DatabaseUpgradeTest extends MangoTestBase {

    @Test
    public void doUpgrade() {

    }

    public static class UpgradeConfig {
        @Bean
        @Primary
        public DatabaseProxy databaseProxy(DatabaseProxyConfiguration configuration) {
            return new H2InMemoryDatabaseProxy(configuration,
                    () -> getClass().getResourceAsStream("version1/createTables-H2.sql"),
                    () -> getClass().getResourceAsStream("version1/defaultData-H2.sql"));
        }
    }

    @Override
    protected MockMangoLifecycle getLifecycle() {
        MockMangoLifecycle lifecycle = super.getLifecycle();
        lifecycle.addRuntimeContextConfiguration(UpgradeConfig.class);
        return lifecycle;
    }
}
