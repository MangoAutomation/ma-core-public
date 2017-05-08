/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.io.serial;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Terry Packer
 * 
 */
public abstract class SerialPortProxy {
    public static final int PARITY_NONE = 0;
    public static final int STOPBITS_1 = 1;
    public static final int DATABITS_8 = 8;
    public static final int FLOWCONTROL_NONE = 0;

    protected SerialParameters parameters;

    //Listeners for events (Currently only rx events)
    protected List<SerialPortProxyEventListener> listeners;

    private final String name;
    private final Object closeLock = new Object();
    private volatile boolean closed = true;

    public SerialPortProxy(String name) {
        super();
        this.name = name;
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
     * 
     * @throws SerialPortException
     */
    public void close() throws SerialPortException {
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
    public abstract void closeImpl() throws SerialPortException;

    /**
     * Open a port only if it is currently closed. Throw exception if already open
     * 
     * @throws SerialPortException
     */
    public void open() throws SerialPortException {
        synchronized (closeLock) {
            if (!closed)
                throw new SerialPortException("Serial Port: " + name + " Already Open!");

            openImpl();
            this.closed = false;
        }

    }

    /**
     * Open Implementation of Proxy
     * 
     * @throws SerialPortException
     */
    public abstract void openImpl() throws SerialPortException;

    public abstract SerialPortInputStream getInputStream();

    public abstract SerialPortOutputStream getOutputStream();

    public SerialParameters getParameters() {
        return parameters;
    }

    public void setParameters(SerialParameters parameters) {
        this.parameters = parameters;
    }

    public void addEventListener(SerialPortProxyEventListener listener) {
        this.listeners.add(listener);
    }

    public void removeEventListener(SerialPortProxyEventListener listener) {
        this.listeners.remove(listener);
    }
}
