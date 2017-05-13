/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.module.definitions.settings;

import java.util.ArrayList;
import java.util.List;

import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.module.SystemSettingsListenerDefinition;
import com.serotonin.m2m2.rt.maint.work.BackupWorkItem;

/**
 * Re-schedule backup if the Hour or Minute change
 * @author Terry Packer
 */
public class BackupSettingsListenerDefinition extends SystemSettingsListenerDefinition{

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.vo.systemSettings.SystemSettingsListener#SystemSettingsSaved(java.lang.String, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void SystemSettingsSaved(String key, Object oldValue, Object newValue) {
        //Reschedule the task if we are supposed to
        BackupWorkItem.unschedule();
        if (SystemSettingsDao.getBooleanValue(SystemSettingsDao.BACKUP_ENABLED,true))
            BackupWorkItem.schedule();
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
		keys.add(SystemSettingsDao.BACKUP_MINUTE);
		keys.add(SystemSettingsDao.BACKUP_HOUR);
		keys.add(SystemSettingsDao.BACKUP_ENABLED);
		keys.add(SystemSettingsDao.BACKUP_PERIOD_TYPE);
		keys.add(SystemSettingsDao.BACKUP_PERIODS);
		return keys;
	}

}
