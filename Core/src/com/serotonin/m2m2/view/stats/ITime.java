/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.view.stats;

/**
 * Interface for NoSQL Time Series Objects
 * 
 * Enforcement to have a time to store against
 * 
 * @author Terry Packer
 *
 */
public interface ITime {

	/**
	 * Get the time
	 * @return
	 */
    long getTime();
}
