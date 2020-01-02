/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.db;

import org.junit.Test;

import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.MockMangoLifecycle;

/**
 * Perform a full upgrade from version 1 of the schema on an H2 database.
 *
 * @author Terry Packer
 */
public class H2DatabaseUpgradeTest extends MangoTestBase {

    private final String createScript = "version1/createTables-H2.sql";
    private final String dataScript = "version1/defaultData-H2.sql";

    @Test
    public void doUpgrade() {

    }

    //TODO Insert Test Data!
    protected void insertTestData() {

    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.MangoTestBase#getLifecycle()
     */
    @Override
    protected MockMangoLifecycle getLifecycle() {
        MockMangoLifecycle lifecycle = new MockMangoLifecycle(modules);
        H2InMemoryDatabaseProxy proxy = new H2InMemoryDatabaseProxy(true, 8000, false, createScript, dataScript);
        lifecycle.setDb(proxy);
        return lifecycle;
    }
}
