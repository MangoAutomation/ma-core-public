/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.io.serial;

import java.io.IOException;

import jssc.SerialPort;
import jssc.SerialPortException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Terry Packer
 * 
 */
public class JsscSerialPortOutputStream extends SerialPortOutputStream{
	private final Logger LOG = LoggerFactory.getLogger(JsscSerialPortOutputStream.class);
    private SerialPort port;

    public JsscSerialPortOutputStream(SerialPort port) {
        this.port = port;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.io.OutputStream#write(int)
     */
    @Override
    public void write(int arg0) throws IOException {
        try {
            byte b = (byte) arg0;
        	if (LOG.isDebugEnabled())
                LOG.debug("Writing byte: " + String.format("%02x", b) );
            if ((port != null) && (port.isOpened())) {
                port.writeByte(b);
            }
        }
        catch (SerialPortException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void flush() {
    	if (LOG.isDebugEnabled())
            LOG.debug("Called no-op flush...");
        //Nothing yet
    }

}

