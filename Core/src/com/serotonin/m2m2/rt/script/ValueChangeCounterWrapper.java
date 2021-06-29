/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.script;

import com.infiniteautomation.mango.statistics.ValueChangeCounter;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;

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
        return statistics.getPeriodStartTime();
    }

    public long getPeriodEndTime() {
        return statistics.getPeriodEndTime();

    }
    
    public String  getStartValue() {
        DataValue value = statistics.getStartValue(); 
        if(value == null)
            return null;
        else
            return value.getStringValue();
    }

    public String getFirstValue() {
        DataValue value = statistics.getFirstValue(); 
    	if(value == null)
    		return null;
    	else
    		return value.getStringValue();
    }

    public Long getFirstTime() {
    	return statistics.getFirstTime();
    }

    public String getLastValue() {
    	if(statistics.getLastValue() == null)
    		return null;
    	else
    		return statistics.getLastValue().getStringValue();
    }

    public Long getLastTime() {
    	return statistics.getLastTime();
    }

    public int getCount() {
        return statistics.getCount();
    }

    public int getChanges() {
    	return statistics.getChanges();
    }

    public String getHelp() {
        return statistics.getHelp();
    }

    @Override
    public String toString() {
    	return statistics.toString();
    }
	
	

}
