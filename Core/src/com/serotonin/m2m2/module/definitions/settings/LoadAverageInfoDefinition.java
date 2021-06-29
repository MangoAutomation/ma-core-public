/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module.definitions.settings;

import com.infiniteautomation.mango.spring.components.ServerMonitoringService;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.module.SystemInfoDefinition;

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
        return (Double)Common.MONITORED_VALUES.getMonitor(ServerMonitoringService.LOAD_AVERAGE_MONITOR_ID).getValue();
    }

    @Override
    public String getDescriptionKey() {
        return "systemInfo.loadAverageDesc";
    }

}
