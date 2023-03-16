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
    @BeforeClass
    public static void setupModule() {
        addModule("SysAccessorTestModule", new SystemSettingsAccessorSchemaDefinitionTest());
        loadModules();
    }

    @Test
    public void testInsertNewModules () {
        SystemSettingsDao dao =  Common.getBean(SystemSettingsDao.class);
        String version = dao.getValue("databaseSchemaVersion.SysAccessorTestModule");
        assertEquals("1", version);
    }
}
