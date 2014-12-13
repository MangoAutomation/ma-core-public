/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.script;

import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.view.stats.ValueChangeCounter;

/**
 * @author Terry Packer
 *
 */
public class ValueChangeCounterWrapper {
	
	private ValueChangeCounter statistics;

	/**
	 * @param statistics
	 */
	public ValueChangeCounterWrapper(ValueChangeCounter statistics) {
		this.statistics = statistics;
	}

    public long getPeriodStartTime() {
        return this.statistics.getPeriodStartTime();
    }

    public long getPeriodEndTime() {
        return this.statistics.getPeriodEndTime();

    }

    public DataValue getFirstValue() {
        return this.statistics.getFirstValue();
    }

    public long getFirstTime() {
    	if(this.statistics.getFirstTime() == null)
    		return -1;
    	else
    		return (long)this.statistics.getFirstTime();
    }

    public DataValue getLastValue() {
    	return this.statistics.getLastValue();
    }

    public long getLastTime() {
    	if(this.statistics.getLastTime() == null)
    		return -1;
    	else
    		return (long)this.statistics.getLastTime();
    }

    public int getCount() {
        return this.statistics.getCount();
    }

    public int getChanges() {
    	return this.statistics.getChanges();
    }

    public String getHelp() {
        return this.statistics.getHelp();
    }

    @Override
    public String toString() {
    	return this.statistics.toString();
    }
	
	

}
