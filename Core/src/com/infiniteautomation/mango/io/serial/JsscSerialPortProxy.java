/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.io.serial;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.m2m2.Common;

import jssc.SerialPort;

/**
 * @author Terry Packer
 *
 */
public class JsscSerialPortProxy extends SerialPortProxy {
	
    private final Logger LOG = LoggerFactory.getLogger(JsscSerialPortProxy.class);

    private int baudRate = -1;
    private FlowControl flowControlIn = FlowControl.NONE;
    private FlowControl flowControlOut = FlowControl.NONE;
    private DataBits dataBits = DataBits.DATA_BITS_8;
    private StopBits stopBits = StopBits.STOP_BITS_1;
    private Parity parity = Parity.NONE;
    
    private SerialPort port;
    private SerialPortOutputStream os;
    private SerialPortInputStream is;

    /**
     * Package Private method to ensure that the only way ports are used is via the manager
     */
    JsscSerialPortProxy(SerialPortIdentifier id, int baudRate, FlowControl flowControlIn,
			FlowControl flowControlOut, DataBits dataBits, StopBits stopBits, Parity parity) {
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

    @Override
    public void writeInt(int arg0) throws SerialPortException {
        try {
            this.port.writeInt(arg0);
        }
        catch (jssc.SerialPortException e) {
            throw new SerialPortException(e);
        }
    }

    @Override
    protected void closeImpl() throws SerialPortException {
        Throwable ex = null;

        try {
            this.is.close();
        }
        catch (IOException e) {
            LOG.error("An error occurred", e);
            ex = e;
        }
        try {
            this.os.close();
        }
        catch (IOException e) {
            LOG.error("An error occurred", e);
            ex = e;
        }
        try {
            this.port.closePort();
        }
        catch (jssc.SerialPortException e) {
            LOG.error("An error occurred", e);
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
            this.port.setFlowControlMode(createFlowControlMode(flowControlIn , flowControlOut));
            this.port.setParams(baudRate, createDataBits(dataBits), createStopBits(stopBits), createParity(parity));
            
            try{
                this.port.purgePort(SerialPort.PURGE_RXCLEAR | SerialPort.PURGE_TXCLEAR);
            }catch(jssc.SerialPortException e){ }

            long period = Common.envProps.getLong("serial.port.linux.readPeriods", 100);
            TimeUnit unit = TimeUnit.valueOf(Common.envProps.getString("serial.port.linux.readPeriodType", "NANOSECONDS"));

            this.is = new JsscSerialPortInputStream(this.port, period, unit, this.listeners);
            this.os = new JsscSerialPortOutputStream(this.port);

        }
        catch (jssc.SerialPortException e) {
            throw new SerialPortException(e);
        }

    }
    
    /**
     */
    public int createParity(Parity parity) {
        switch(parity) {
            case EVEN:
                return SerialPort.PARITY_EVEN;
            case ODD:
                return SerialPort.PARITY_ODD;
            case MARK:
                return SerialPort.PARITY_MARK;
            case SPACE:
                return SerialPort.PARITY_SPACE;
            case NONE:    
                return SerialPort.PARITY_NONE;
            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     */
    public int createStopBits(StopBits stopBits) {
        switch(stopBits) {
            case STOP_BITS_1:
                return SerialPort.STOPBITS_1;
            case STOP_BITS_1_5:
                return SerialPort.STOPBITS_1_5;
            case STOP_BITS_2:
                return SerialPort.STOPBITS_2;
            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     */
    public int createDataBits(DataBits dataBits) {
        switch(dataBits) {
            case DATA_BITS_5:
                return SerialPort.DATABITS_5;
            case DATA_BITS_6:
                return SerialPort.DATABITS_6;
            case DATA_BITS_7:
                return SerialPort.DATABITS_7;
            case DATA_BITS_8:
                return SerialPort.DATABITS_8;
            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     * Specifically massage into JSSC format
     */
    public static int createFlowControlMode(FlowControl in, FlowControl out) {
        int flowIn = SerialPort.FLOWCONTROL_NONE;
        switch(in) {
            case NONE:
                flowIn = SerialPort.FLOWCONTROL_NONE;
                break;
            case RTSCTS:
                flowIn = SerialPort.FLOWCONTROL_RTSCTS_IN;
                break;
            case XONXOFF:
                flowIn = SerialPort.FLOWCONTROL_XONXOFF_IN;
                break;
        }
        
        int flowOut = SerialPort.FLOWCONTROL_NONE;
        switch(out) {
            case NONE:
                flowOut = SerialPort.FLOWCONTROL_NONE;
                break;
            case RTSCTS:
                flowOut = SerialPort.FLOWCONTROL_RTSCTS_OUT;
                break;
            case XONXOFF:
                flowOut = SerialPort.FLOWCONTROL_XONXOFF_OUT;
                break;
        }
        return flowIn | flowOut;
    }

    @Override
    public SerialPortInputStream getInputStream() {
        return this.is;
    }

    @Override
    public SerialPortOutputStream getOutputStream() {
        return this.os;
    }
}
