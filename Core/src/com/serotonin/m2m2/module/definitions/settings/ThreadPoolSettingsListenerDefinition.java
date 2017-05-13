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

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.vo.systemSettings.SystemSettingsListener#SystemSettingsSaved(java.lang.String, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void SystemSettingsSaved(String key, Object oldValue, Object newValue) {
		Integer value = (Integer)newValue;
		switch(key){
		case SystemSettingsDao.HIGH_PRI_CORE_POOL_SIZE:
        	Common.backgroundProcessing.setHighPriorityServiceCorePoolSize(value);
			break;
		case SystemSettingsDao.HIGH_PRI_MAX_POOL_SIZE:
            Common.backgroundProcessing.setHighPriorityServiceMaximumPoolSize(value);
			break;
		case SystemSettingsDao.MED_PRI_CORE_POOL_SIZE:
		case SystemSettingsDao.MED_PRI_MAX_POOL_SIZE:
        	//Due to the pool type we should set these to the same values
        	Common.backgroundProcessing.setMediumPriorityServiceMaximumPoolSize(value);
        	Common.backgroundProcessing.setMediumPriorityServiceCorePoolSize(value);
			break;
		case SystemSettingsDao.LOW_PRI_CORE_POOL_SIZE:
		case SystemSettingsDao.LOW_PRI_MAX_POOL_SIZE:
        	//Due to the pool type we should set these to the same values
        	Common.backgroundProcessing.setLowPriorityServiceMaximumPoolSize(value);
        	Common.backgroundProcessing.setLowPriorityServiceCorePoolSize(value);
			break;
		}
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.vo.systemSettings.SystemSettingsListener#SystemSettingsRemoved(java.lang.String, java.lang.Object)
	 */
	@Override
	public void SystemSettingsRemoved(String key, Object lastValue) {
		//NoOp
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.vo.systemSettings.SystemSettingsListener#getKeys()
	 */
	@Override
	public List<String> getKeys() {
		List<String> keys = new ArrayList<String>();
		keys.add(SystemSettingsDao.HIGH_PRI_CORE_POOL_SIZE);
		keys.add(SystemSettingsDao.HIGH_PRI_MAX_POOL_SIZE);
		
		keys.add(SystemSettingsDao.MED_PRI_MAX_POOL_SIZE);
		keys.add(SystemSettingsDao.MED_PRI_MAX_POOL_SIZE);
		
		keys.add(SystemSettingsDao.LOW_PRI_MAX_POOL_SIZE);
		keys.add(SystemSettingsDao.LOW_PRI_MAX_POOL_SIZE);
		
		return keys;
	}

}
