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
}
