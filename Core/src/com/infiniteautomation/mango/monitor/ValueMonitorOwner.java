/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 *
 */
package com.infiniteautomation.mango.monitor;

/**
 * The source and owner of a ValueMonitor, used to reset/refresh the value when required.
 * 
 * Using the ID the corresponding ValueMonitor should be reset
 * 
 * @author Terry Packer
 */
public interface ValueMonitorOwner{

	/**
	 * Reload the value from its source
	 * Useful for counters that can get out of sync with their external source.
	 */
	public void reset(String monitorId);
}
