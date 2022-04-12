/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.io.serial;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.m2m2.Common;

import jssc.SerialNativeInterface;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;


/**
 *
 * Serial port input stream that reads the serial bytes into a queue and fires events to listeners
 *
 * Windows machines use interrupt driven events, all others poll the port for new data
 * See configuration properties
 *  serial.port.eventQueueSize
 *  serial.port.linux.readPeriods
 *  serial.port.linux.readPeriodType
 *
 * @author Terry Packer
 *
 */
public class JsscSerialPortInputStream extends SerialPortInputStream implements SerialPortEventListener {

    private final Logger LOG = LoggerFactory.getLogger(JsscSerialPortInputStream.class);
    protected final SerialPort port;
    protected final List<SerialPortProxyEventListener> listeners;
    protected final ScheduledFuture<?> reader;

    /**
     */
    public JsscSerialPortInputStream(SerialPort serialPort, long readPollPeriod, TimeUnit readPollPeriodType, List<SerialPortProxyEventListener> listeners)
            throws jssc.SerialPortException {
        this.listeners = listeners;
        this.port = serialPort;

        //This is ok on windows, linux we want to poll on our own
        if(SerialNativeInterface.getOsType() == SerialNativeInterface.OS_WINDOWS) {
            this.port.addEventListener(this, SerialPort.MASK_RXCHAR);
            this.reader = null;
        }else {
            //Setup a listener task to fire events
            this.reader = JsscSerialPortManager.instance.addReader(()-> {
                try {
                    if (this.listeners.size() > 0) {
                        int cnt = this.port.getInputBufferBytesCount();
                        if (cnt > 0) {
                            this.serialEvent(new SerialPortEvent(this.port, SerialPort.MASK_RXCHAR, cnt));
                        }
                    }
                }catch (jssc.SerialPortException e) {
                    LOG.error("An error occurred", e);
                }catch(Exception e) {
                    LOG.error("An error occurred", e);
                }
            }, readPollPeriod, readPollPeriodType);
        }

        if(LOG.isDebugEnabled())
            LOG.debug("Creating Jssc Serial Port Input Stream for: " + serialPort.getPortName());
    }

    @Override
    public int read() throws IOException {
        try {
            if(this.port.getInputBufferBytesCount() > 0) {
                byte[] bytes = this.port.readBytes(1);
                if (bytes != null && bytes.length > 0) {
                    return 0xFF & bytes[0];
                } else {
                    return -1;
                }
            }else {
                return -1;
            }
        } catch (SerialPortException e) {
            throw new IOException(e);
        }
    }

    @Override
    public int available() throws IOException {
        try {
            return this.port.getInputBufferBytesCount();
        } catch (SerialPortException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void closeImpl() throws IOException {
        if(SerialNativeInterface.getOsType() == SerialNativeInterface.OS_WINDOWS) {
            try {
                this.port.removeEventListener(); //Remove the listener
            }
            catch (jssc.SerialPortException e) {
                throw new IOException(e);
            }
        }else {
            if(this.reader != null) {
                try {
                    this.reader.cancel(true);
                }catch(Exception e) {
                    LOG.error("Serial port read thread for port " + this.port.getPortName() + " failed to stop", e);
                }
            }
        }
    }

    /**
     * Serial Event Executed (Used for Windows Machines)
     */
    @Override
    public void serialEvent(SerialPortEvent event) {
        if (event.isRXCHAR()) {//If data is available
            if (LOG.isDebugEnabled())
                LOG.debug("Serial Receive Event fired.");
            //We used to Read the bytes, store into queue, the idea is that this event
            // will cause the listener to read the port for at least the event number of bytes
            if (listeners.size() > 0) {
                //Create a new RX Event
                final SerialPortProxyEvent upstreamEvent = new SerialPortProxyEvent(Common.timer.currentTimeMillis(), event.getEventValue());
                for (final SerialPortProxyEventListener listener : listeners){
                    SerialPortProxyEventTask task = new SerialPortProxyEventTask(listener, upstreamEvent);
                    if(!JsscSerialPortManager.instance.addEvent(task))
                        LOG.error("Serial Port Problem, Listener task queue full, data will be lost!  Increase serial.port.eventQueueSize to avoid this.");
                }
            }

        }//end was RX event
        else {
            if (LOG.isDebugEnabled())
                LOG.debug("Non RX Event Type Recieved: " + event.getEventType());
        }
    }
}
