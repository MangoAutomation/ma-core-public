/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.script;

import com.infiniteautomation.mango.statistics.ValueChangeCounter;

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

    public String getFirstValue() {
    	if(statistics.getFirstValue() == null)
    		return null;
    	else
    		return this.statistics.getFirstValue().getStringValue();
    }

    public long getFirstTime() {
    	if(this.statistics.getFirstTime() == null)
    		return -1;
    	else
    		return (long)this.statistics.getFirstTime();
    }

    public String getLastValue() {
    	if(statistics.getLastValue() == null)
    		return null;
    	else
    		return this.statistics.getLastValue().getStringValue();
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
