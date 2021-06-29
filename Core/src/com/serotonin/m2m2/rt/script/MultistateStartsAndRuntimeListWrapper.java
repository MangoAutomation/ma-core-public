/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.script;

import java.util.Map;

import com.infiniteautomation.mango.statistics.StartsAndRuntime;
import com.infiniteautomation.mango.statistics.StartsAndRuntimeList;
import com.serotonin.m2m2.rt.dataImage.types.MultistateValue;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;

/**
 * @author Terry Packer
 *
 */
public class MultistateStartsAndRuntimeListWrapper extends StartsAndRuntimeListWrapper{

	/**
	 * @param statistics
	 */
	public MultistateStartsAndRuntimeListWrapper(StartsAndRuntimeList statistics) {
		super(statistics);
	}
	
	public Integer getStartValue() {
	    DataValue value = getStartDataValue();
        if(value == null)
            return null;
        else
            return value.getIntegerValue();
    }
    public Integer getFirstValue() {
        DataValue value = getFirstDataValue(); 
    	if(value == null)
    		return null;
    	else
    		return value.getIntegerValue();
    }
    public Integer getLastValue() {
        DataValue value = getStartDataValue();
        if(value == null)
            return null;
        else
            return value.getIntegerValue();
    }
    public StartsAndRuntime get(int value) {
        Map<Object, StartsAndRuntime> values = statistics.getStartsAndRuntime();
        if(values.containsKey(value))
            return values.get(value);
        else
            return new StartsAndRuntime(new MultistateValue(value));
    }
    @Override
    public String toString() {
        return super.toString() + " get(int): StartsAndRuntime";
    }
}
