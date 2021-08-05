/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.io.serial.virtual;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.infiniteautomation.mango.io.serial.SerialPortInputStream;
import com.serotonin.ShouldNeverHappenException;

public class SerialServerSocketBridgeInputStream extends SerialPortInputStream {
    private static final Logger LOG = LoggerFactory.getLogger(SerialServerSocketBridgeInputStream.class);
    
	private InputStream stream = null;
	private ByteArrayInputStream bufferStream = null;
	private final int bufferSize;
	private int currentBuffered = 0;
	private AtomicInteger currentPosition = new AtomicInteger(0);
	
	public SerialServerSocketBridgeInputStream(int bufferSize) {
		super();
		this.bufferSize = bufferSize;
	}
	
	public void connect(InputStream in) {
		this.stream = in;
	}
	
	public int bufferRead() throws IOException, SerialServerSocketConnectionClosedException {
	    synchronized(this) {	
	        if(stream == null)
    			return -1;
    		if(bufferStream != null)
    			return bufferStream.available();
    		//Add one so we can insert a -1 at the end and close our buffer stream
    		byte[] data = new byte[bufferSize];
    		int read;
    		try {
    		    read = stream.read(data, 0, data.length);
    		} catch(IOException e) {
    		    throw new SerialServerSocketConnectionClosedException(e);
    		}
    		if(read == -1)
    			throw new SerialServerSocketConnectionClosedException();
    		currentBuffered = read;
    		currentPosition.set(0);
    		bufferStream = new ByteArrayInputStream(data);
    		if(LOG.isDebugEnabled())
    		    LOG.debug("SSSBIS: received " + read + " characters into buffer: " + new String(data));
    	    return read;
		}
	}
	
	@Override
	public int read() throws IOException {
	    synchronized(this) {
    		if(this.bufferStream == null) {
    			try { 
    				bufferRead(); 
    			} catch(SerialServerSocketConnectionClosedException e) {
    				return -1;
    			}
    			if(this.bufferStream == null)
    				return -1;
    		}
    		
    		if(currentPosition.getAndIncrement() == currentBuffered) {
    			this.bufferStream.close();
    			this.bufferStream = null;
    			return -1;
    		}
    		return this.bufferStream.read();
	    }
	}

	@Override
	public int available() throws IOException {
	    synchronized(this) {
    		if(this.bufferStream == null)
    			return 0;
    		return this.bufferStream.available();
	    }
	}

	@Override
	public void closeImpl() throws IOException {
		synchronized(this) {
		    if(this.stream != null)
	            this.stream.close();
    		if(this.bufferStream != null)
    			this.bufferStream.close();
		}
	}

	@Override
	public int peek() {
		throw new ShouldNeverHappenException("Unimplemented.");
	}

}
