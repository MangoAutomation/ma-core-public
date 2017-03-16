/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.rt.dataImage;

import com.serotonin.m2m2.view.stats.ITime;

/**
 * 
 * @author Terry Packer
 */
public interface IdTime extends ITime{

	/**
	 * Get the ID for the VO in Question
	 * @return
	 */
	public int getId();
	
}
