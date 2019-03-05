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
public class ThreadPoolSettingsListenerDefinition extends SystemSettingsListenerDefinition{

	@Override
	public void systemSettingsSaved(String key, String oldValue, String newValue) {
        Integer value = Integer.parseInt(newValue);
        switch (key) {
            case SystemSettingsDao.HIGH_PRI_CORE_POOL_SIZE:
                Common.backgroundProcessing.setHighPriorityServiceCorePoolSize(value);
                break;
            case SystemSettingsDao.HIGH_PRI_MAX_POOL_SIZE:
                Common.backgroundProcessing.setHighPriorityServiceMaximumPoolSize(value);
                break;
            case SystemSettingsDao.MED_PRI_CORE_POOL_SIZE:
                Common.backgroundProcessing.setMediumPriorityServiceCorePoolSize(value);
                break;
            case SystemSettingsDao.LOW_PRI_CORE_POOL_SIZE:
                Common.backgroundProcessing.setLowPriorityServiceCorePoolSize(value);
                break;
        }
	}

	@Override
	public void systemSettingsRemoved(String key, String lastValue, String defaultValue) {
		//NoOp
	}
	
	@Override
	public List<String> getKeys() {
		List<String> keys = new ArrayList<String>();
		keys.add(SystemSettingsDao.HIGH_PRI_CORE_POOL_SIZE);
		keys.add(SystemSettingsDao.HIGH_PRI_MAX_POOL_SIZE);
		keys.add(SystemSettingsDao.MED_PRI_CORE_POOL_SIZE);
		keys.add(SystemSettingsDao.LOW_PRI_CORE_POOL_SIZE);
		
		return keys;
	}

}
