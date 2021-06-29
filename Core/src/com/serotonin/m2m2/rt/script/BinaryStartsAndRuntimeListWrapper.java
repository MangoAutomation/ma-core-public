/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.script;

import java.util.Map;

import com.infiniteautomation.mango.statistics.StartsAndRuntime;
import com.infiniteautomation.mango.statistics.StartsAndRuntimeList;
import com.serotonin.m2m2.rt.dataImage.types.BinaryValue;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;

/**
 * @author Terry Packer
 *
 */
public class BinaryStartsAndRuntimeListWrapper extends StartsAndRuntimeListWrapper{

	/**
	 * @param statistics
	 */
	public BinaryStartsAndRuntimeListWrapper(StartsAndRuntimeList statistics) {
		super(statistics);
	}

	public Boolean getStartValue() {
	    DataValue value = getStartDataValue();
        if(value == null)
            return null;
        else
            return value.getBooleanValue();
    }
    public Boolean getFirstValue() {
        DataValue value = getFirstDataValue();
        if(value == null)
            return null;
        else
            return value.getBooleanValue();
    }
    public Boolean getLastValue() {
        DataValue value = getLastDataValue();
        if(value == null)
            return null;
        else
            return value.getBooleanValue();
    }
    public StartsAndRuntime get(boolean value) {
        Map<Object, StartsAndRuntime> values = statistics.getStartsAndRuntime();
        if(values.containsKey(value))
            return values.get(value);
        else
            return new StartsAndRuntime(new BinaryValue(value));
    }
    @Override
    public String toString() {
        return super.toString() + " get(boolean): StartsAndRuntime";
    }
}
