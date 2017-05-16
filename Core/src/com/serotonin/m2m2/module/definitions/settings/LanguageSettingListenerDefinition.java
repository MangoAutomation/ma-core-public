/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
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

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.vo.systemSettings.SystemSettingsListener#SystemSettingsSaved(java.lang.String, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void SystemSettingsSaved(String key, String oldValue, String newValue) {
		Common.setSystemLanguage(newValue);
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.vo.systemSettings.SystemSettingsListener#SystemSettingsRemoved(java.lang.String, java.lang.Object)
	 */
	@Override
	public void SystemSettingsRemoved(String key, String lastValue) {
		//NoOp
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.vo.systemSettings.SystemSettingsListener#getKeys()
	 */
	@Override
	public List<String> getKeys() {
		List<String> keys = new ArrayList<String>();
		keys.add(SystemSettingsDao.LANGUAGE);
		return keys;
	}

}
