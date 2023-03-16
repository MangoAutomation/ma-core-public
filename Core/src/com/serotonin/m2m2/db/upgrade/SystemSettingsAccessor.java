/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.upgrade;

import java.util.Optional;

import org.jooq.DSLContext;

import com.infiniteautomation.mango.db.tables.SystemSettings;

/**
 * Used to get/set system settings before DAOs are initialized
 */
@FunctionalInterface
public interface SystemSettingsAccessor {

    DSLContext getContext();

    default SystemSettings systemSettingsTable() {
        return SystemSettings.SYSTEM_SETTINGS;
    }

    default Optional<String> getSystemSetting(String key) {
        SystemSettings systemSettings = systemSettingsTable();
        return getContext().select(systemSettings.settingValue)
                .from(systemSettings)
                .where(systemSettings.settingName.eq(key))
                .fetchOptional(systemSettings.settingValue);
    }

    default void setSystemSetting(String key, String value) {
        SystemSettings table = systemSettingsTable();
        switch (getContext().dialect()) {
            case MYSQL:
            case MARIADB:
                // the @Supports annotation on mergeInto claims that it supports MySQL, however it does not
                // translate/emulate the merge using "on duplicate key update" so it fails
                getContext().insertInto(table)
                        .columns(table.settingValue, table.settingName)
                        .values(value, key)
                        .onDuplicateKeyUpdate()
                        .set(table.settingValue, value)
                        .execute();
            case POSTGRES:
                getContext().insertInto(table)
                        .columns(table.settingValue, table.settingName)
                        .values(value, key)
                        .onConflict(table.settingName)
                        .doUpdate()
                        .set(table.settingValue, value)
                        .execute();
            default:
                getContext().mergeInto(table)
                        .using(getContext().selectOne())
                        .on(table.settingName.eq(key))
                        .whenMatchedThenUpdate()
                        .set(table.settingValue, value)
                        .whenNotMatchedThenInsert(table.settingValue, table.settingName)
                        .values(value, key)
                        .execute();
        }

    }
}
