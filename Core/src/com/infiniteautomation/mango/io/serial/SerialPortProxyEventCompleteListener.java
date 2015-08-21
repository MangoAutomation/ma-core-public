/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.io.serial;

/**
 * @author Terry Packer
 *
 */
public interface SerialPortProxyEventCompleteListener {

	/**
	 * Event fired when event has completed
	 * @param time
	 */
	public void eventComplete(long time, SerialPortProxyEventTask task);
	
}

