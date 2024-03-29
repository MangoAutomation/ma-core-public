/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module.definitions.settings;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.module.SystemInfoDefinition;
import com.serotonin.m2m2.rt.maint.work.DatabaseBackupWorkItem;

/**
 * 
 * @author Terry Packer
 */
public class SqlDatabaseBackupFileListInfoDefinition extends SystemInfoDefinition<List<String>>{

	@Autowired
	protected SystemSettingsDao systemSettingsDao;

	public final String KEY = "sqlDatabaseBackupFileList";

	@Override
	public String getKey() {
		return KEY;
	}

	@Override
	public List<String> getValue() {
		List<String> filenames = new ArrayList<>();
        String backupLocation = systemSettingsDao.getValue(SystemSettingsDao.DATABASE_BACKUP_FILE_LOCATION);
        File[] backupFiles = DatabaseBackupWorkItem.getBackupFiles(backupLocation);
        
        if(backupFiles == null) {
        	return filenames;
        }

        //Parse the list into data for a select list
        //Files of form core-database-TYPE-date_time
        for (File file : backupFiles) {
            String filename = file.getName();
            filenames.add(filename);
        }
        
        return filenames;
	}

    @Override
    public String getDescriptionKey() {
        return "systemInfo.sqlDatabaseBackupListDesc";
    }

}
