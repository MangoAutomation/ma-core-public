/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.io.serial;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.infiniteautomation.mango.io.serial.virtual.VirtualSerialPortConfig;
import com.infiniteautomation.mango.io.serial.virtual.VirtualSerialPortConfig.SerialPortTypes;
import com.infiniteautomation.mango.io.serial.virtual.VirtualSerialPortConfigDao;
import com.serotonin.m2m2.Common;
import com.serotonin.util.LifecycleException;

import jssc.SerialNativeInterface;
import jssc.SerialPortList;


/**
 * @author Terry Packer
 *
 */
public class SerialPortManagerImpl implements SerialPortManager {

    private final Logger LOG = LoggerFactory.getLogger(SerialPortManagerImpl.class);

    private final ReadWriteLock lock;
    private final List<SerialPortIdentifier> freePorts;
    private final List<SerialPortIdentifier> ownedPorts;
    private volatile boolean initialized;
    private volatile boolean portsLoaded; //For lazy loading of ports

    public SerialPortManagerImpl() {
        lock = new ReentrantReadWriteLock();
        freePorts = new CopyOnWriteArrayList<SerialPortIdentifier>();
        ownedPorts = new CopyOnWriteArrayList<SerialPortIdentifier>();
        initialized = false;
        portsLoaded = false;
    }

    @Override
    public List<SerialPortIdentifier> getFreeCommPorts() throws Exception {
        if (!portsLoaded) {
            this.lock.writeLock().lock();
            try {
                if(!portsLoaded) {
                    refreshPorts();
                    portsLoaded = true;
                }
            } finally {
                this.lock.writeLock().unlock();
            }
        }
        return freePorts;
    }

    @Override
    public List<SerialPortIdentifier> getAllCommPorts() throws Exception {
        if (!portsLoaded) {
            this.lock.writeLock().lock();
            try {
                if(!portsLoaded) {
                    refreshPorts();
                    portsLoaded = true;
                }
            } finally {
                this.lock.writeLock().unlock();
            }
        }
        List<SerialPortIdentifier> allPorts = new ArrayList<SerialPortIdentifier>(freePorts);
        allPorts.addAll(ownedPorts);
        return allPorts;
    }

    @Override
    public void refreshFreeCommPorts() throws Exception {
        this.lock.writeLock().lock();
        try {
            freePorts.clear();
            refreshPorts();
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    /**
     * This will only start the threading infrastructure for the ports, the actual 'Lazy load' of all ports is done if/when serial devices are used'
     */
    @Override
    public void initialize(boolean safe) throws LifecycleException {
        this.lock.writeLock().lock();
        try {
            if(initialized) {
                throw new LifecycleException("Serial Port Manager should only be initialized once");
            }

            JsscSerialPortManager.instance.initialize();
            initialized = true;
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    /**
     * Refresh the ports list
     * @throws UnsatisfiedLinkError
     * @throws NoClassDefFoundError
     */
    protected void refreshPorts() throws UnsatisfiedLinkError,  NoClassDefFoundError {
        String[] portNames;
        Map<String, Boolean> portOwnership = new HashMap<String, Boolean>();

        switch (SerialNativeInterface.getOsType()) {
            case SerialNativeInterface.OS_LINUX:
                portNames = SerialPortList.getPortNames(
                        Common.envProps.getString("serial.port.linux.path", "/dev/"),
                        Pattern.compile(Common.envProps.getString("serial.port.linux.regex",
                                "((cu|ttyS|ttyUSB|ttyACM|ttyAMA|rfcomm|ttyO|COM)[0-9]{1,3}|rs(232|485)-[0-9])")));
                break;
            case SerialNativeInterface.OS_MAC_OS_X:
                portNames = SerialPortList.getPortNames(
                        Common.envProps.getString("serial.port.osx.path", "/dev/"),
                        Pattern.compile(Common.envProps.getString("serial.port.osx.regex",
                                "(cu|tty)..*"))); // Was "tty.(serial|usbserial|usbmodem).*")
                break;
            case SerialNativeInterface.OS_WINDOWS:
                portNames = SerialPortList.getPortNames(
                        Common.envProps.getString("serial.port.windows.path", ""),
                        Pattern.compile(
                                Common.envProps.getString("serial.port.windows.regex", "")));
                break;
            default:
                portNames = SerialPortList.getPortNames();
                break;
        }

        for (SerialPortIdentifier port : ownedPorts)
            portOwnership.put(port.getName(), true);

        for (String portName : portNames) {
            if (!portOwnership.containsKey(portName)) {
                freePorts.add(new SerialPortIdentifier(portName, SerialPortTypes.JSSC));
                portOwnership.put(portName, false);
            } else if(LOG.isDebugEnabled())
                LOG.debug("Not adding port " + portName + " to free ports because it is owned.");
        }

        // Collect any Virtual Comm Ports from the DB and load them in
        List<VirtualSerialPortConfig> list = VirtualSerialPortConfigDao.getInstance().getAll();
        if (list != null) {
            for (VirtualSerialPortConfig config : list) {
                if(!portOwnership.containsKey(config.getPortName())) {
                    freePorts.add(new VirtualSerialPortIdentifier(config));
                    portOwnership.put(config.getPortName(), false);
                } else if(LOG.isWarnEnabled()) {
                    LOG.warn("Virtual serial port config " + config.getXid() + " named " + config.getPortName() +
                            " not available due to name conflict with other serial port or it was open during refresh.");
                }
            }
        }
    }

    @Override
    public boolean portOwned(String commPortId) {
        if (!portsLoaded) {
            this.lock.writeLock().lock();
            try {
                if(!portsLoaded) {
                    refreshPorts();
                    portsLoaded = true;
                }
            } finally {
                this.lock.writeLock().unlock();
            }
        }
        // Check to see if the port is currently in use.
        for (SerialPortIdentifier id : ownedPorts) {
            if (id.getName().equalsIgnoreCase(commPortId)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String getPortOwner(String commPortId) {
        if (!portsLoaded) {
            this.lock.writeLock().lock();
            try {
                if(!portsLoaded) {
                    refreshPorts();
                    portsLoaded = true;
                }
            } finally {
                this.lock.writeLock().unlock();
            }
        }
        // Get serial port owner
        for (SerialPortIdentifier id : ownedPorts) {
            if (id.getName().equalsIgnoreCase(commPortId))
                return id.getCurrentOwner();
        }

        return null;
    }

    @Override
    public boolean isPortNameRegexMatch(String portName) {
        switch (SerialNativeInterface.getOsType()) {
            case SerialNativeInterface.OS_LINUX:
                return Pattern.matches(Common.envProps.getString("serial.port.linux.regex",
                        "((cu|ttyS|ttyUSB|ttyACM|ttyAMA|rfcomm|ttyO|COM)[0-9]{1,3}|rs(232|485)-[0-9])"),
                        portName);
            case SerialNativeInterface.OS_MAC_OS_X:
                return Pattern.matches(
                        Common.envProps.getString("serial.port.osx.regex", "(cu|tty)..*"),
                        portName); // Was "tty.(serial|usbserial|usbmodem).*")
            case SerialNativeInterface.OS_WINDOWS:
                return Pattern.matches(Common.envProps.getString("serial.port.windows.regex", ""),
                        portName);
            default:
                return false;
        }
    }

    @Override
    public SerialPortProxy open(String ownerName, String commPortId, int baudRate,
            FlowControl flowControlIn, FlowControl flowControlOut, DataBits dataBits, StopBits stopBits, Parity parity)
                    throws SerialPortException {

        this.lock.writeLock().lock();
        try {
            if (!initialized) {
                initialize(false);
            }
            if(!portsLoaded) {
                refreshPorts();
                portsLoaded = true;
            }
            // Check to see if the port is currently in use.
            for (SerialPortIdentifier id : ownedPorts) {
                if (id.getName().equalsIgnoreCase(commPortId)) {
                    throw new SerialPortException(
                            "Port " + commPortId + " in use by " + id.getCurrentOwner());
                }
            }

            // Get the Free port
            SerialPortIdentifier portId = null;
            for (SerialPortIdentifier id : freePorts) {
                if (id.getName().equalsIgnoreCase(commPortId)) {
                    portId = id;
                    break;
                }
            }

            // Did we find it?
            if (portId == null)
                //Well let's try to open it and see what happens, assume it's JSSC
                portId = new SerialPortIdentifier(commPortId, SerialPortTypes.JSSC);

            // Try to open the port
            SerialPortProxy port;
            switch (portId.getType()) {
                case SerialPortTypes.JSSC:
                    port = new JsscSerialPortProxy(portId, baudRate, flowControlIn, flowControlOut,
                            dataBits, stopBits, parity);
                    break;
                case SerialPortTypes.SERIAL_SOCKET_BRIDGE:
                case SerialPortTypes.SERIAL_SERVER_SOCKET_BRIDGE:
                    VirtualSerialPortIdentifier bridgeId = (VirtualSerialPortIdentifier) portId;
                    port = bridgeId.getProxy();
                    break;
                default:
                    throw new SerialPortException("Uknown port type " + portId.getType());
            }

            port.open();
            // Port is open move to owned list
            freePorts.remove(portId);
            portId.setCurrentOwner(ownerName);
            portId.setPort(port);
            ownedPorts.add(portId);

            return port;
        } catch (Exception e) {
            // Wrap all exceptions
            if (e instanceof SerialPortException)
                throw (SerialPortException) e;
            throw new SerialPortException(e);
        } finally {
            lock.writeLock().unlock();
        }
    }


    // TODO Run a periodic task to check when the port was last accessed and if past some time then
    // assume it is not owned anymore?
    @Override
    public void close(SerialPortProxy port) throws SerialPortException {

        lock.writeLock().lock();

        try {
            if (!initialized)
                initialize(false);

            if(!portsLoaded) {
                refreshPorts();
                portsLoaded = true;
            }

            // Close the port
            if (port == null)
                return; // Can't close a non existent port

            port.close();

            SerialPortIdentifier id = port.getSerialPortIdentifier();
            if (this.ownedPorts.remove(id)) {
                id.setCurrentOwner("");
                id.setPort(null);
                this.freePorts.add(id);
            } else {
                // It must have already been free?
                if (!this.freePorts.contains(id)) {
                    throw new SerialPortException("Port " + id.getName() + " was already free?");
                }
            }

        } catch (Exception e) {
            // Wrap all exceptions
            if (e instanceof SerialPortException)
                throw (SerialPortException) e;
            throw new SerialPortException(e);
        } finally {
            lock.writeLock().unlock();
        }

    }

    @Override
    public void terminate() throws LifecycleException {
        lock.writeLock().lock();
        try {
            // Close all the ports if they are open
            for (SerialPortIdentifier id : ownedPorts) {
                try {
                    id.getPort().close();
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                }
            }

            ownedPorts.clear();

            //Shutdown JSSC Manager
            JsscSerialPortManager.instance.terminate();
            initialized = false;
        } catch (Exception e) {
            throw e;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void joinTermination() {
        JsscSerialPortManager.instance.joinTermination();
    }

}
