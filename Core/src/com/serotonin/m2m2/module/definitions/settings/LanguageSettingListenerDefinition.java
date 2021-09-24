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
 * 
 * @author Terry Packer
 */
public class LanguageSettingListenerDefinition extends SystemSettingsListenerDefinition{

	@Override
	public void systemSettingsSaved(String key, String oldValue, String newValue) {
        if (newValue == null)
            newValue = SystemSettingsDao.getInstance().getValue(SystemSettingsDao.LANGUAGE);
		Common.setSystemLanguage(newValue);
	}

	@Override
	public void systemSettingsRemoved(String key, String lastValue, String defaultValue) {
		//NoOp
	}

	@Override
	public List<String> getKeys() {
		List<String> keys = new ArrayList<String>();
		keys.add(SystemSettingsDao.LANGUAGE);
		return keys;
	}

}
