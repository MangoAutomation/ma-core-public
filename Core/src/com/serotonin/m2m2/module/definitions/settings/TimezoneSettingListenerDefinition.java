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
 * @author Mert Cingöz
 */
public class TimezoneSettingListenerDefinition extends SystemSettingsListenerDefinition {

    @Override
    public void systemSettingsSaved(String key, String oldValue, String newValue) {
        if (newValue == null)
            newValue = SystemSettingsDao.instance.getValue(SystemSettingsDao.TIMEZONE);
        Common.setSystemTimezone(newValue);
    }

    @Override
    public void systemSettingsRemoved(String key, String lastValue, String defaultValue) {
        //NoOp
    }

    @Override
    public List<String> getKeys() {
        List<String> keys = new ArrayList<String>();
        keys.add(SystemSettingsDao.TIMEZONE);
        return keys;
    }

}
