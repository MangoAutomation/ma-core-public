/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.io.serial;

import com.infiniteautomation.mango.io.serial.virtual.VirtualSerialPortConfig;

/**
 * Useful to Mimic a serial port for Mango
 * 
 * @author Terry Packer
 *
 */
public class VirtualSerialPortIdentifier extends SerialPortIdentifier{

	private VirtualSerialPortConfig config;
	
	/**
	 * @param name
	 * @param type
	 */
	public VirtualSerialPortIdentifier(VirtualSerialPortConfig config) {
		super(config.getPortName(), config.getType());
		this.config = config;
	}

	public SerialPortProxy getProxy(){
		return this.config.createProxy(this);
	}
	
}
