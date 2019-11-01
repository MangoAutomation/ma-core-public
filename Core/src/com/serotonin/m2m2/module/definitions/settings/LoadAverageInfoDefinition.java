/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.module.definitions.settings;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.module.Module;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.SystemInfoDefinition;
import com.serotonin.m2m2.rt.maint.WorkItemMonitor;

/**
 * Class to define Read only settings/information that can be provided
 * 
 * @author Terry Packer
 */
public class LoadAverageInfoDefinition extends SystemInfoDefinition<Double>{

	public final String KEY = "loadAverage";
	
	@Override
	public String getKey() {
		return KEY;
	}

	@Override
	public Double getValue() {
	    return (Double)Common.MONITORED_VALUES.getValueMonitor(WorkItemMonitor.LOAD_AVERAGE_MONITOR_ID).getValue();
	}

	
	@Override
	public Module getModule() {
		return ModuleRegistry.getCoreModule();
	}

    @Override
    public String getDescriptionKey() {
        return "systemInfo.loadAverageDesc";
    }

}
