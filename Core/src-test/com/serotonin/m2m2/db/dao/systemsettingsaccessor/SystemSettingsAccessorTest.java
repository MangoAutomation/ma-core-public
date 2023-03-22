/*
 * Copyright (C) 2023 RadixIot LLC. All rights reserved.
 *
 */

package com.serotonin.m2m2.db.dao.systemsettingsaccessor;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;

public class SystemSettingsAccessorTest extends MangoTestBase {
    /**
     * Here we load our schema definition which would represent a New module
     */
    @BeforeClass
    public static void setupModule() {
        addModule("SysAccessorTestModule", new SystemSettingsAccessorSchemaDefinitionTest());
        loadModules();
    }

    /**
     * This test has been made because there was a bug where new modules getting installed for the first time wouldn't get added to the system settings table since the set method only has a create and not and update. A solution has been implemented to allow upserting (updating/creating) and this test is what confirms this bug is no longer around.
     * So Once the testing module/SchemaDefinition has been loaded we need to check that the module has been saved to the system settings table with the first version
     */
    @Test
    public void testInsertNewModules () {
        SystemSettingsDao dao =  Common.getBean(SystemSettingsDao.class);
        String version = dao.getValue("databaseSchemaVersion.SysAccessorTestModule");
        assertEquals("1", version);
    }
}
