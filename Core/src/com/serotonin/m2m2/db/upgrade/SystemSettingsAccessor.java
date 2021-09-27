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
        SystemSettings systemSettings = systemSettingsTable();
        getContext().update(systemSettings)
                .set(systemSettings.settingValue, value)
                .where(systemSettings.settingName.eq(key))
                .execute();
    }
}
