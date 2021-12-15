/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
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
     */
	public VirtualSerialPortIdentifier(VirtualSerialPortConfig config) {
		super(config.getPortName(), config.getType());
		this.config = config;
	}

	public SerialPortProxy getProxy(){
		return this.config.createProxy(this);
	}
	
}
