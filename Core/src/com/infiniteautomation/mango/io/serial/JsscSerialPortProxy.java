/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.io.serial;

import java.io.IOException;

import jssc.SerialPort;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Terry Packer
 *
 */
public class JsscSerialPortProxy extends SerialPortProxy {
	
    private final Log LOG = LogFactory.getLog(JsscSerialPortProxy.class);

    private int baudRate = -1;
    private int flowControlIn = FLOWCONTROL_NONE;
    private int flowControlOut = FLOWCONTROL_NONE;
    private int dataBits = DATABITS_8;
    private int stopBits = STOPBITS_1;
    private int parity = PARITY_NONE;
    
    private SerialPort port;
    private SerialPortOutputStream os;
    private SerialPortInputStream is;

    /**
     * Package Private method to ensure that the only way ports are used is via the manager
     * @param serialParameters
     */
    JsscSerialPortProxy(SerialPortIdentifier id, int baudRate, int flowControlIn,
			int flowControlOut, int dataBits, int stopBits, int parity) {
        super(id);
        
        this.baudRate = baudRate;
        this.flowControlIn = flowControlIn;
        this.flowControlOut = flowControlOut;
        this.dataBits = dataBits;
        this.stopBits = stopBits;
        this.parity = parity;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.serotonin.io.serial.SerialPortProxy#readBytes(int)
     */
    @Override
    public byte[] readBytes(int i) throws SerialPortException {
        try {
            return this.port.readBytes(i);
        }
        catch (jssc.SerialPortException e) {
            throw new SerialPortException(e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.serotonin.io.serial.SerialPortProxy#writeInt(int)
     */
    @Override
    public void writeInt(int arg0) throws SerialPortException {
        try {
            this.port.writeInt(arg0);
        }
        catch (jssc.SerialPortException e) {
            throw new SerialPortException(e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.serotonin.io.serial.SerialPortProxy#close()
     */
    @Override
    protected void closeImpl() throws SerialPortException {
        Throwable ex = null;

        try {
            this.is.close();
        }
        catch (IOException e) {
            LOG.error(e);
            ex = e;
        }
        try {
            this.os.close();
        }
        catch (IOException e) {
            LOG.error(e);
            ex = e;
        }
        try {
            this.port.closePort();
        }
        catch (jssc.SerialPortException e) {
            LOG.error(e);
            ex = e;
        }

        if (ex != null)
            throw new SerialPortException(ex); //May miss some errors if > 1, but hey we get something back. 

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.serotonin.io.serial.SerialPortProxy#open()
     */
    @Override
    protected void openImpl() throws SerialPortException {

        try {
        	if (LOG.isDebugEnabled())
                LOG.debug("Opening Serial Port: " + commPortId);
        	
            this.port = new SerialPort(commPortId.getName());

            this.port.openPort();
            this.port.setFlowControlMode(flowControlIn | flowControlOut);
            this.port.setParams(baudRate, dataBits, stopBits,parity);
            
            this.is = new JsscSerialPortInputStream(this.port, this.listeners);
            this.os = new JsscSerialPortOutputStream(this.port);

        }
        catch (jssc.SerialPortException e) {
            throw new SerialPortException(e);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.serotonin.io.serial.SerialPortProxy#getInputStream()
     */
    @Override
    public SerialPortInputStream getInputStream() {
        return this.is;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.serotonin.io.serial.SerialPortProxy#getOutputStream()
     */
    @Override
    public SerialPortOutputStream getOutputStream() {
        return this.os;
    }
    
	public String getPortOwnerName() {
		return commPortId.getCurrentOwner();
	}

	public int getBaudRate() {
		return baudRate;
	}

	public void setBaudRate(int baudRate) {
		this.baudRate = baudRate;
	}

	public int getFlowControlIn() {
		return flowControlIn;
	}

	public void setFlowControlIn(int flowControlIn) {
		this.flowControlIn = flowControlIn;
	}

	public int getFlowControlOut() {
		return flowControlOut;
	}

	public void setFlowControlOut(int flowControlOut) {
		this.flowControlOut = flowControlOut;
	}

	public int getDataBits() {
		return dataBits;
	}

	public void setDataBits(int dataBits) {
		this.dataBits = dataBits;
	}

	public int getStopBits() {
		return stopBits;
	}

	public void setStopBits(int stopBits) {
		this.stopBits = stopBits;
	}

	public int getParity() {
		return parity;
	}

	public void setParity(int parity) {
		this.parity = parity;
	}
    
}
