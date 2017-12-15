/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.script;

import com.infiniteautomation.mango.statistics.StartsAndRuntimeList;

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

    public int getFirstValue() {
    	if(statistics.getFirstValue() == null)
    		return 0;
    	else
    		return statistics.getFirstValue().getIntegerValue();
    }
    public int getLastValue() {
    	if(statistics.getLastValue() == null)
    		return 0;
    	else
    		return statistics.getLastValue().getIntegerValue();
    }
}
