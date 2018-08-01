/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.script;

import com.infiniteautomation.mango.statistics.StartsAndRuntimeList;
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
}
