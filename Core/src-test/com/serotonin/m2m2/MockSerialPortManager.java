/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2;

import java.util.List;

import com.infiniteautomation.mango.io.serial.DataBits;
import com.infiniteautomation.mango.io.serial.FlowControl;
import com.infiniteautomation.mango.io.serial.Parity;
import com.infiniteautomation.mango.io.serial.SerialPortException;
import com.infiniteautomation.mango.io.serial.SerialPortIdentifier;
import com.infiniteautomation.mango.io.serial.SerialPortManager;
import com.infiniteautomation.mango.io.serial.SerialPortProxy;
import com.infiniteautomation.mango.io.serial.StopBits;
import com.serotonin.util.LifecycleException;

/**
 * Dummy serial port manager for use in testing.
 *
 * @author Terry Packer
 */
public class MockSerialPortManager implements SerialPortManager {

    /* (non-Javadoc)
     * @see com.serotonin.util.ILifecycle#joinTermination()
     */
    @Override
    public void joinTermination() {
        
    }

    /* (non-Javadoc)
     * @see com.infiniteautomation.mango.io.serial.SerialPortManager#getFreeCommPorts()
     */
    @Override
    public List<SerialPortIdentifier> getFreeCommPorts() throws Exception {

        return null;
    }

    /* (non-Javadoc)
     * @see com.infiniteautomation.mango.io.serial.SerialPortManager#getAllCommPorts()
     */
    @Override
    public List<SerialPortIdentifier> getAllCommPorts() throws Exception {

        return null;
    }

    /* (non-Javadoc)
     * @see com.infiniteautomation.mango.io.serial.SerialPortManager#refreshFreeCommPorts()
     */
    @Override
    public void refreshFreeCommPorts() throws Exception {
        
    }

    /* (non-Javadoc)
     * @see com.infiniteautomation.mango.io.serial.SerialPortManager#initialize(boolean)
     */
    @Override
    public void initialize(boolean safe) throws LifecycleException {
        
    }

    /* (non-Javadoc)
     * @see com.infiniteautomation.mango.io.serial.SerialPortManager#portOwned(java.lang.String)
     */
    @Override
    public boolean portOwned(String commPortId) {

        return false;
    }

    /* (non-Javadoc)
     * @see com.infiniteautomation.mango.io.serial.SerialPortManager#getPortOwner(java.lang.String)
     */
    @Override
    public String getPortOwner(String commPortId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.infiniteautomation.mango.io.serial.SerialPortManager#isPortNameRegexMatch(java.lang.String)
     */
    @Override
    public boolean isPortNameRegexMatch(String portName) {

        return false;
    }
    
    @Override
    public SerialPortProxy open(String ownerName, String commPortId, int baudRate,
            FlowControl flowControlIn, FlowControl flowControlOut, DataBits dataBits,
            StopBits stopBits, Parity parity) throws SerialPortException {
        // TODO Auto-generated method stub
        return null;
    }
    
    /* (non-Javadoc)
     * @see com.infiniteautomation.mango.io.serial.SerialPortManager#close(com.infiniteautomation.mango.io.serial.SerialPortProxy)
     */
    @Override
    public void close(SerialPortProxy port) throws SerialPortException {
        
    }

    /* (non-Javadoc)
     * @see com.infiniteautomation.mango.io.serial.SerialPortManager#terminate()
     */
    @Override
    public void terminate() throws LifecycleException {
        
    }

}
