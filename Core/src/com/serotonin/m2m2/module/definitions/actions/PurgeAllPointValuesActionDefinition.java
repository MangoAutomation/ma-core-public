/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.module.definitions.actions;

import com.fasterxml.jackson.databind.JsonNode;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.module.SystemActionDefinition;
import com.serotonin.m2m2.util.timeout.SystemActionTask;
import com.serotonin.timer.OneTimeTrigger;

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
	public SystemActionTask getTask(final JsonNode input) {
		return new PurgeAllPointValuesAction();
	}
	
	/**
	 * Class to allow purging data in ordered tasks with a queue 
	 * of up to 5 waiting purges
	 * 
	 * @author Terry Packer
	 */
	class PurgeAllPointValuesAction extends SystemActionTask{
		
		public PurgeAllPointValuesAction(){
			super(new OneTimeTrigger(0l), "Purge All Point Values", "ALL_POINT_VALUE_PURGE", 5);
		}

		/* (non-Javadoc)
		 * @see com.serotonin.timer.Task#run(long)
		 */
		@Override
		public void runImpl(long runtime) {
			long cnt = Common.runtimeManager.purgeDataPointValues();
			this.results.put("deleted", cnt);
		}
	}
}
