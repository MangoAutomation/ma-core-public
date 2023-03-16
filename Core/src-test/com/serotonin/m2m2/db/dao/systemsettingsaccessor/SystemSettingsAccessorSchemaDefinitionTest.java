/*
 * Copyright (C) 2023 RadixIot LLC. All rights reserved.
 *
 */
package com.serotonin.m2m2.db.dao.systemsettingsaccessor;

import java.util.List;

import org.jooq.Table;

import com.infiniteautomation.mango.db.DefaultSchema;
import com.serotonin.m2m2.module.DatabaseSchemaDefinition;

public class SystemSettingsAccessorSchemaDefinitionTest extends DatabaseSchemaDefinition {

    @Override
    public String getNewInstallationCheckTableName() {
        return "testmoduletable";
    }

    @Override
    public List<Table<?>> getTablesForConversion() {
        return DefaultSchema.DEFAULT_SCHEMA.getTables();
    }

    @Override
    public String getUpgradePackage() {
        return "com.serotonin.m2m2.db.dao.systemsettingsaccessor.upgrade";
    }

    @Override
    public int getDatabaseSchemaVersion() {
        return 1;
    }
}
