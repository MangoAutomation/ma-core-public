/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.script;

import com.serotonin.m2m2.view.stats.StartsAndRuntimeList;

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

    public boolean getFirstValue() {
    	if(statistics.getFirstValue() == null)
    		return false;
    	else
    		return statistics.getFirstValue().getBooleanValue();
    }
    public boolean getLastValue() {
    	if(statistics.getLastValue() == null)
    		return false;
    	else
    		return statistics.getLastValue().getBooleanValue();
    }
}
