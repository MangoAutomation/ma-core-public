/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module.definitions.settings;

import java.util.ArrayList;
import java.util.List;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.module.SystemSettingsListenerDefinition;

/**
 * Sets Common.lastUpgrade
 * @author Jared Wiltshire
 */
public class LastUpgradeSettingsListenerDefinition extends SystemSettingsListenerDefinition{

    @Override
    public void systemSettingsSaved(String key, String oldValue, String newValue) {
        if (SystemSettingsDao.LAST_UPGRADE.equals(key)) {
            Common.LAST_UPGRADE.reset();
        }
    }

    @Override
    public void systemSettingsRemoved(String key, String lastValue, String defaultValue) {

    }

    @Override
    public List<String> getKeys() {
        List<String> keys = new ArrayList<String>();
        keys.add(SystemSettingsDao.LAST_UPGRADE);
        return keys;
    }
}
