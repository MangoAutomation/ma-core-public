/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.module.definitions.settings;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

import com.serotonin.m2m2.module.Module;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.SystemInfoDefinition;

/**
 * Class to define Read only settings/information that can be provided
 * 
 * @author Terry Packer
 */
public class LoadAverageInfoDefinition extends SystemInfoDefinition<Double>{

	public final String KEY = "loadAverage";
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.ReadOnlySettingDefinition#getName()
	 */
	@Override
	public String getKey() {
		return KEY;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.ReadOnlySettingDefinition#getValue()
	 */
	@Override
	public Double getValue() {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        if(osBean != null)
        	return osBean.getSystemLoadAverage();
        else
        	return 0D;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.module.ModuleElementDefinition#getModule()
	 */
	@Override
	public Module getModule() {
		return ModuleRegistry.getCoreModule();
	}

    @Override
    public String getDescriptionKey() {
        return "systemInfo.loadAverageDesc";
    }

}
