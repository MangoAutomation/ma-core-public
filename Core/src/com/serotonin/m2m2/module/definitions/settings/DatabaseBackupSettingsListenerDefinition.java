/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module.definitions.settings;

import java.util.ArrayList;
import java.util.List;

import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.module.SystemSettingsListenerDefinition;
import com.serotonin.m2m2.rt.maint.work.DatabaseBackupWorkItem;

/**
 * Re-schedule backup if the Hour or Minute change
 * @author Terry Packer
 */
public class DatabaseBackupSettingsListenerDefinition extends SystemSettingsListenerDefinition{

	@Override
	public void systemSettingsSaved(String key, String oldValue, String newValue) {
        //Reschedule the task
	    synchronized(this) {
            DatabaseBackupWorkItem.unschedule();
            if (SystemSettingsDao.getInstance().getBooleanValue(SystemSettingsDao.DATABASE_BACKUP_ENABLED))
                DatabaseBackupWorkItem.schedule();
	    }
	}

	@Override
	public void systemSettingsRemoved(String key, String lastValue, String defaultValue) {
		//NoOp
	}

	@Override
	public List<String> getKeys() {
		List<String> keys = new ArrayList<String>();
		keys.add(SystemSettingsDao.DATABASE_BACKUP_MINUTE);
		keys.add(SystemSettingsDao.DATABASE_BACKUP_HOUR);
		keys.add(SystemSettingsDao.DATABASE_BACKUP_ENABLED);
		keys.add(SystemSettingsDao.DATABASE_BACKUP_PERIOD_TYPE);
		keys.add(SystemSettingsDao.DATABASE_BACKUP_PERIODS);
		return keys;
	}

}
