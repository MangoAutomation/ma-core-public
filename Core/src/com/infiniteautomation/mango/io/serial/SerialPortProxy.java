/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.io.serial;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Terry Packer
 *
 */
public abstract class SerialPortProxy {

    protected SerialPortIdentifier commPortId;
    
    //Listeners for events (Currently only rx events)
    protected List<SerialPortProxyEventListener> listeners;

    private final Object closeLock = new Object();
    private volatile boolean closed = true;

    public SerialPortProxy(SerialPortIdentifier commPortId) {
        this.commPortId = commPortId;
        this.listeners = new ArrayList<SerialPortProxyEventListener>();
    }

    /**
     * @param i
     * @return
     */
    public abstract byte[] readBytes(int i) throws SerialPortException;

    /**
     * @param arg0
     */
    public abstract void writeInt(int arg0) throws SerialPortException;

    /**
     * Close a serial port only once
     *  - package private -
     * @throws SerialPortException
     */
    void close() throws SerialPortException {
        synchronized (closeLock) {
            if (closed) {
                return;
            }
            closeImpl();
            closed = true;
        }
    }

    /**
     * Close Implementation of Proxy
     * 
     * @throws SerialPortException
     */
    protected abstract void closeImpl() throws SerialPortException;

    /**
     * Open a port only if it is currently closed. Throw exception if already open
     * - package private -
     * @throws SerialPortException
     */
    void open() throws SerialPortException {
        synchronized (closeLock) {
            if (!closed)
                throw new SerialPortException("Serial Port: " + commPortId.getName() + " Already Open!");

            openImpl();
            this.closed = false;
        }

    }
    
    /**
     * Get the identifier
     * 
     * - package private -
     * @return
     */
    SerialPortIdentifier getSerialPortIdentifier(){
    	return this.commPortId;
    }

    /**
     * Open Implementation of Proxy
     * 
     * @throws SerialPortException
     */
    protected abstract void openImpl() throws SerialPortException;

    public abstract SerialPortInputStream getInputStream();

    public abstract SerialPortOutputStream getOutputStream();

    public void addEventListener(SerialPortProxyEventListener listener) {
        this.listeners.add(listener);
    }

    public void removeEventListener(SerialPortProxyEventListener listener) {
        this.listeners.remove(listener);
    }

	public String getCommPortId() {
		return commPortId.getName();
	}
	
	public String getOwner(){
		return commPortId.getCurrentOwner();
	}
    
}
