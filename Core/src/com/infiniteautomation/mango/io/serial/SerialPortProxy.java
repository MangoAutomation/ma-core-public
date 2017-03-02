/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.io.serial;

import java.util.ArrayList;
import java.util.List;

import com.serotonin.ShouldNeverHappenException;

/**
 * @author Terry Packer
 *
 */
public abstract class SerialPortProxy {
    
	public enum Parity {
		
		NONE,
		ODD,
		EVEN,
		MARK,
		SPACE;

		public static int getJsscParityValue(Parity value){
			switch(value){
			case NONE:
				return 0;
			case ODD:
				return 1;
			case EVEN:
				return 2;
			case MARK:
				return 3;
			case SPACE:
				return 4;
			default:
			throw new ShouldNeverHappenException(" Un-convertable parity value.");
			}
		}

		/**
		 * @param parity
		 * @return
		 */
		public static Parity fromJsscValue(int parity) {
			switch(parity){
			case 0:
				return NONE;
			case 1:
				return ODD;
			case 2:
				return EVEN;
			case 3:
				return MARK;
			case 4:
				return SPACE;
			default:
			throw new ShouldNeverHappenException(" Un-convertable parity value.");
			}
		}
	}
	
    public static final int PARITY_NONE = 0;
    public static final int STOPBITS_1 = 1;
    public static final int DATABITS_8 = 8;
    public static final int FLOWCONTROL_NONE = 0;
    
    //TODO Pull in other static defs from jssc.SerialPort

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
