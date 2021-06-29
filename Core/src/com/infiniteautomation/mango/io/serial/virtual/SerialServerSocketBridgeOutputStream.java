/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.io.serial.virtual;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.infiniteautomation.mango.io.serial.SerialPortOutputStream;

public class SerialServerSocketBridgeOutputStream extends SerialPortOutputStream {

	private final static Log LOG = LogFactory.getLog(SerialServerSocketBridgeOutputStream.class);
	OutputStream stream = null;
	
	public void connect(OutputStream out) {
		this.stream = out;
	}
	
	@Override
	public void write(int arg0) throws IOException {
		if(this.stream == null)
			throw new IOException("Socket connection not established.");
		if(LOG.isTraceEnabled())
		    LOG.trace("SSSBOS: sending byte: " + arg0);
		this.stream.write(arg0);
	}

	@Override
	public void flush() {
		if(this.stream == null)
			return;
		try {
			this.stream.flush();
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
	}
}
