/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.script;

import java.util.List;
import java.util.Map;

import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.view.stats.StartsAndRuntime;
import com.serotonin.m2m2.view.stats.StartsAndRuntimeList;

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

    public long getFirstTime() {
    	if(this.statistics.getFirstTime() == null)
    		return -1;
    	else
    		return (long)this.statistics.getFirstTime();
    }

    protected DataValue getLastDataValue() {
        return this.statistics.getLastValue();
    }

    public long getLastTime() {
    	if(this.statistics.getLastTime() == null)
    		return -1;
    	else
    		return (long)this.statistics.getLastTime();
    }

    public Map<Object, StartsAndRuntime> getStartsAndRuntime() {
       return this.statistics.getStartsAndRuntime();
    }

    public List<StartsAndRuntime> getData() {
        return this.statistics.getData();
    }

    public StartsAndRuntime get(Object value) {
        return this.statistics.get(DataValue.objectToValue(value));
    }

    public StartsAndRuntime get(DataValue value) {
    	return this.statistics.get(value);
    }

    public String getHelp() {
        return this.statistics.getHelp();
    }

    @Override
    public String toString() {
    	return this.statistics.toString();
    }
	
}
