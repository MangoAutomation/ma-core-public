/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
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