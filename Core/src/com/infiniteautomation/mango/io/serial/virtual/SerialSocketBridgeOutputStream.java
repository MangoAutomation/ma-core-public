/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.io.serial.virtual;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.infiniteautomation.mango.io.serial.SerialPortOutputStream;

/**
 * Class for the output stream of a the connection for a serial port to a Client Socket/Inet Address
 * 
 * @author Terry Packer
 *
 */
public class SerialSocketBridgeOutputStream extends SerialPortOutputStream {

	private final static Log LOG = LogFactory.getLog(SerialSocketBridgeOutputStream.class);
	
	private OutputStream stream;
	
	public SerialSocketBridgeOutputStream(OutputStream os){
		this.stream = os;
	}
	
	@Override
	public void write(int arg0) throws IOException {
		this.stream.write(arg0);
	}

	@Override
	public void flush() {
		try {
			this.stream.flush();
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
	}

}
