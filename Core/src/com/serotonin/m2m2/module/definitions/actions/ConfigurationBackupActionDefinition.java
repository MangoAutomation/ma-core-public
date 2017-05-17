/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.module.definitions.actions;

import com.fasterxml.jackson.databind.JsonNode;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.module.SystemActionDefinition;
import com.serotonin.m2m2.rt.maint.work.BackupWorkItem;
import com.serotonin.m2m2.util.timeout.SystemActionTask;
import com.serotonin.timer.OneTimeTrigger;

/**
 * 
 * @author Terry Packer
 */
public class ConfigurationBackupActionDefinition extends SystemActionDefinition{

	private final String KEY = "backupConfiguration";
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.SystemActionDefinition#getKey()
	 */
	@Override
	public String getKey() {
		return KEY;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.SystemActionDefinition#getWorkItem(com.fasterxml.jackson.databind.JsonNode)
	 */
	@Override
	public SystemActionTask getTask(final JsonNode input) {
		return new Action();
	}
	
	/**
	 * Class to allow purging data in ordered tasks with a queue 
	 * of up to 5 waiting purges
	 * 
	 * @author Terry Packer
	 */
	class Action extends SystemActionTask{
		
		private final BackupWorkItem item;
		private volatile boolean cancelled;
		
		public Action(){
			super(new OneTimeTrigger(0l), "Configuration Backup", "CONFIG_BACKUP", 5);
			this.item = new BackupWorkItem();
		}

		/* (non-Javadoc)
		 * @see com.serotonin.timer.Task#run(long)
		 */
		@Override
		public void runImpl(long runtime) {
        	String backupLocation = SystemSettingsDao.getValue(SystemSettingsDao.BACKUP_FILE_LOCATION);
			item.setBackupLocation(backupLocation);
			Common.backgroundProcessing.addWorkItem(item);
			
			//As a medium priority task we can just wait here until we are done
			while(true){
				if(item.isFinished())
					break;
				else
					try {
						Thread.sleep(800);
					} catch (InterruptedException e) { 	}
			}
			
		}
		
		/* (non-Javadoc)
		 * @see com.serotonin.m2m2.util.timeout.SystemActionTask#cancel()
		 */
		@Override
		public boolean cancel() {
			boolean result = super.cancel();
			this.cancelled = true;
			this.item.cancel();
			return result;
		}
		
		/* (non-Javadoc)
		 * @see com.serotonin.timer.Task#isCancelled()
		 */
		@Override
		public boolean isCancelled() {
			return this.cancelled;
		}
		
	}
}
