/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.io.serial.virtual;

import java.io.IOException;
import java.io.InputStream;

import com.infiniteautomation.mango.io.serial.SerialPortInputStream;

/**
 * Class for the input stream of a serial port to a Client Socket/Inet Address
 * 
 * @author Terry Packer
 *
 */
public class SerialSocketBridgeInputStream extends SerialPortInputStream {

	private InputStream stream;
	
	public SerialSocketBridgeInputStream(InputStream is) {
		this.stream = is;
	}
	
	@Override
	public int read() throws IOException {
		return this.stream.read();
	}

	@Override
	public int available() throws IOException {
		return this.stream.available();
	}

	@Override
	public void closeImpl() throws IOException {
		this.stream.close();
	}
}
