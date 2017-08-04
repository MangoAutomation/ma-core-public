/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.module.definitions.actions;

import com.fasterxml.jackson.databind.JsonNode;
import com.infiniteautomation.mango.rest.v2.exception.ValidationFailedRestException;
import com.infiniteautomation.mango.rest.v2.model.RestValidationResult;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.module.SystemActionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.PurgeWithPurgeSettingsActionPermissionDefinition;
import com.serotonin.m2m2.rt.maint.DataPurge;
import com.serotonin.m2m2.util.timeout.SystemActionTask;
import com.serotonin.timer.OneTimeTrigger;

/**
 * 
 * @author Terry Packer
 */
public class PurgeWithPurgeSettingsActionDefinition extends SystemActionDefinition{

	private final String KEY = "purgeUsingSettings";
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
		return new Action();
	}
	
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.SystemActionDefinition#getPermissionTypeName()
	 */
	@Override
	protected String getPermissionTypeName() {
		return PurgeWithPurgeSettingsActionPermissionDefinition.PERMISSION;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.SystemActionDefinition#validate(com.fasterxml.jackson.databind.JsonNode)
	 */
	@Override
	protected RestValidationResult validateImpl(JsonNode input) throws ValidationFailedRestException {
		return null;
	}
	
	/**
	 * Class to allow purging data in ordered tasks with a queue 
	 * of up to 5 waiting purges
	 * 
	 * @author Terry Packer
	 */
	class Action extends SystemActionTask{
		
		public Action(){
			super(new OneTimeTrigger(0l), "Purge Using Settings", "PURGE_USING_SETTINGS", 5);
		}

		/* (non-Javadoc)
		 * @see com.serotonin.timer.Task#run(long)
		 */
		@Override
		public void runImpl(long runtime) {
	        DataPurge dataPurge = new DataPurge();
	        dataPurge.execute(Common.timer.currentTimeMillis());
			this.results.put("deletedPointValues", dataPurge.getDeletedSamples());
			this.results.put("deletedFiles", dataPurge.getDeletedFiles());
			this.results.put("deletedEvents", dataPurge.getDeletedEvents());
			this.results.put("anyDeletedSamples", dataPurge.isAnyDeletedSamples());
		}
	}
}
