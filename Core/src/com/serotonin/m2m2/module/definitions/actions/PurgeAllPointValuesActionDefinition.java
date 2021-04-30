/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.module.definitions.actions;

import com.fasterxml.jackson.databind.JsonNode;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.module.SystemActionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.PurgeAllPointValuesActionPermissionDefinition;
import com.serotonin.m2m2.util.timeout.SystemActionTask;
import com.serotonin.timer.OneTimeTrigger;
import com.serotonin.util.ILifecycleState;

/**
 * 
 * @author Terry Packer
 */
public class PurgeAllPointValuesActionDefinition extends SystemActionDefinition{

	private final String KEY = "purgeAllPointValues";
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
	public SystemActionTask getTaskImpl(final JsonNode input) {
		return new PurgeAllPointValuesAction();
	}
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.SystemActionDefinition#getPermissionTypeName()
	 */
	@Override
	protected String getPermissionTypeName() {
		return PurgeAllPointValuesActionPermissionDefinition.PERMISSION;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.SystemActionDefinition#validate(com.fasterxml.jackson.databind.JsonNode)
	 */
	@Override
	protected void validate(JsonNode input) throws ValidationException {

	}
	
	/**
	 * Class to allow purging data in ordered tasks with a queue 
	 * of up to 5 waiting purges
	 * 
	 * @author Terry Packer
	 */
	static class PurgeAllPointValuesAction extends SystemActionTask{
		
		public PurgeAllPointValuesAction(){
			super(new OneTimeTrigger(0l), "Purge All Point Values", "ALL_POINT_VALUE_PURGE", 5);
		}

		/* (non-Javadoc)
		 * @see com.serotonin.timer.Task#run(long)
		 */
		@Override
		public void runImpl(long runtime) {
			if(Common.runtimeManager.getLifecycleState() == ILifecycleState.RUNNING){
				boolean countPointValues = SystemSettingsDao.instance.getBooleanValue(SystemSettingsDao.POINT_DATA_PURGE_COUNT);
				if(countPointValues){
					long cnt = Common.runtimeManager.purgeDataPointValues();
					this.results.put("deleted", cnt);
				}else{
					Common.runtimeManager.purgeDataPointValuesWithoutCount();
					this.results.put("deleted", -1);
				}
			}
		}
	}
}
