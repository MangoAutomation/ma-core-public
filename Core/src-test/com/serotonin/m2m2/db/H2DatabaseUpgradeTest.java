/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db;

import java.io.InputStream;
import java.util.function.Supplier;

import org.junit.Test;

import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.MockMangoLifecycle;

/**
 * Perform a full upgrade from version 1 of the schema on an H2 database.
 *
 * @author Terry Packer
 */
public class H2DatabaseUpgradeTest extends MangoTestBase {

    private final Supplier<InputStream> createScript = () -> {
        return H2DatabaseUpgradeTest.class.getResourceAsStream("version1/createTables-H2.sql");
    };

    private final Supplier<InputStream> dataScript = () -> {
        return H2DatabaseUpgradeTest.class.getResourceAsStream("version1/defaultData-H2.sql");
    };

    @Test
    public void doUpgrade() {

    }

    //TODO Insert Test Data!
    protected void insertTestData() {

    }

    @Override
    protected MockMangoLifecycle getLifecycle() {
        MockMangoLifecycle lifecycle = new MockMangoLifecycle(modules);
        H2InMemoryDatabaseProxy proxy = new H2InMemoryDatabaseProxy(true, 8000, false, createScript, dataScript);
        lifecycle.setDb(proxy);
        return lifecycle;
    }
}
