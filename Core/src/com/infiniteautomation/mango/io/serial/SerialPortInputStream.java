/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.io.serial;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Terry Packer
 *
 */
public abstract class SerialPortInputStream extends InputStream{

	private final Logger LOG = LoggerFactory.getLogger(SerialPortInputStream.class);
    private final Object closeLock = new Object();
    private volatile boolean closed = false;

	/* (non-Javadoc)
	 * @see java.io.InputStream#read()
	 */
    @Override
    public abstract int read() throws IOException;

	/* (non-Javadoc)
	 * @see java.io.InputStream#available()
	 */
	@Override
	public abstract int available() throws IOException;

	/* (non-Javadoc)
	 * @see java.io.InputStream#close()
	 */
	@Override
	public void close() throws IOException {
		if (LOG.isDebugEnabled())
            LOG.debug("Attempting Close of Serial Port Input Stream.");
        synchronized (closeLock) {
            if (closed) {
                return;
            }
            closeImpl();
            closed = true;
        }
        if (LOG.isDebugEnabled())
            LOG.debug("Closed Serial Port Input Stream.");
	}
    

	/**
	 * The close implementation for the stream
     */
	public abstract void closeImpl() throws IOException;

	/**
	 * Peek at the head of the stream, do not remove the byte
	 */
	public abstract int peek();
}
