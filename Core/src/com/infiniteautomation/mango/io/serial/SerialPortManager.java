/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.io.serial;

import java.util.List;

import com.serotonin.util.ILifecycle;
import com.serotonin.util.LifecycleException;


/**
 *
 * @author Terry Packer
 */
public interface SerialPortManager extends ILifecycle {

    String VIRTUAL_SERIAL_PORT_KEY = "comm.virtual.serial";

    /**
     * Get a list of all free Comm Ports
     * 
     * @return
     * @throws CommPortConfigException
     */
    List<SerialPortIdentifier> getFreeCommPorts() throws Exception;

    /**
     * Get a list of all free Comm Ports
     * 
     * @return
     * @throws CommPortConfigException
     */
    List<SerialPortIdentifier> getAllCommPorts() throws Exception;

    /**
     * Refresh the list of available Comm Ports
     * 
     * @return
     * @throws CommPortConfigException
     */
    void refreshFreeCommPorts() throws Exception;

    /**
     * 
     * @param safe
     * @throws SerialPortConfigException
     */
    void initialize(boolean safe) throws LifecycleException;

    /**
     * Is a port currently in use by Mango?
     * 
     * @param commPortId
     * @return
     */
    boolean portOwned(String commPortId);

    String getPortOwner(String commPortId);

    boolean isPortNameRegexMatch(String portName);

    /**
     * @param string
     * @param commPortId
     * @param baudRate
     * @param flowControlIn
     * @param flowControlOut
     * @param dataBits
     * @param stopBits
     * @param parity
     * @param parity2
     * @return
     */
    SerialPortProxy open(String ownerName, String commPortId, int baudRate, FlowControl flowControlIn,
            FlowControl flowControlOut, DataBits dataBits, StopBits stopBits, Parity parity) throws SerialPortException;

    /**
     * @param port
     */
    void close(SerialPortProxy port) throws SerialPortException;

    /**
     * Ensure all ports are closed
     */
    void terminate() throws LifecycleException;

}
