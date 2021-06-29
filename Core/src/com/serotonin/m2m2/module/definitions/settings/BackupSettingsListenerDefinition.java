/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
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

	@Override
	public void systemSettingsSaved(String key, String oldValue, String newValue) {
        //Reschedule the task if we are supposed to
	    synchronized(this) {
            BackupWorkItem.unschedule();
            if (SystemSettingsDao.instance.getBooleanValue(SystemSettingsDao.BACKUP_ENABLED))
                BackupWorkItem.schedule();
	    }
	}

	@Override
	public void systemSettingsRemoved(String key, String lastValue, String defaultValue) {
		//NoOp
	}

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
