/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.script;

import java.util.List;
import java.util.Map;

import com.infiniteautomation.mango.statistics.StartsAndRuntime;
import com.infiniteautomation.mango.statistics.StartsAndRuntimeList;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;

/**
 * @author Terry Packer
 *
 */
public abstract class StartsAndRuntimeListWrapper {

	protected StartsAndRuntimeList statistics;

	/**
	 * @param statistics
	 */
	public StartsAndRuntimeListWrapper(StartsAndRuntimeList statistics) {
		this.statistics = statistics;
	}
	
    public long getPeriodStartTime() {
        return this.statistics.getPeriodStartTime();
    }

    public long getPeriodEndTime() {
        return this.statistics.getPeriodEndTime();
    }
    
    protected DataValue getStartDataValue() {
        return this.statistics.getStartValue();
    }

    protected DataValue getFirstDataValue() {
        return this.statistics.getFirstValue();
    }

    public Long getFirstTime() {
    	return this.statistics.getFirstTime();
    }

    protected DataValue getLastDataValue() {
        return this.statistics.getLastValue();
    }

    public Long getLastTime() {
    	return this.statistics.getLastTime();
    }

    public Map<Object, StartsAndRuntime> getStartsAndRuntime() {
       return this.statistics.getStartsAndRuntime();
    }

    public List<StartsAndRuntime> getData() {
        return this.statistics.getData();
    }

    public String getHelp() {
        return this.statistics.getHelp();
    }

    @Override
    public String toString() {
    	return this.statistics.toString();
    }
	
}
