/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.io.serial;

/**
 * @author Terry Packer
 *
 */
public interface SerialPortProxyEventListener {

	/**
	 * @param upstreamEvent
	 */
	public void serialEvent(SerialPortProxyEvent upstreamEvent);

}