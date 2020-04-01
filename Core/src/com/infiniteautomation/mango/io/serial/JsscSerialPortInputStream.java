/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.io.serial;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.io.StreamUtils;
import com.serotonin.m2m2.Common;

import jssc.SerialNativeInterface;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;


/**
 *
 * Serial port input stream that reads the serial bytes into a queue and fires events to listeners
 *
 * Windows machines use interrupt driven events, all others poll the port for new data
 * @see env.properties
 *  serial.port.eventQueueSize
 *  serial.port.linux.readPeriods
 *  serial.port.linux.readPeriodType
 *
 * @author Terry Packer
 *
 */
public class JsscSerialPortInputStream extends SerialPortInputStream implements SerialPortEventListener {

    private final Log LOG = LogFactory.getLog(JsscSerialPortInputStream.class);
    protected final LinkedBlockingQueue<Byte> dataStream;
    protected final SerialPort port;
    protected final List<SerialPortProxyEventListener> listeners;
    protected final ScheduledFuture<?> reader;

    /**
     * @param serialPort
     * @throws SerialPortException
     */
    public JsscSerialPortInputStream(SerialPort serialPort, long readPollPeriod, TimeUnit readPollPeriodType, List<SerialPortProxyEventListener> listeners)
            throws jssc.SerialPortException {
        this.listeners = listeners;
        this.dataStream = new LinkedBlockingQueue<Byte>();

        this.port = serialPort;

        //This is ok on windows, linux we want to poll on our own
        if(SerialNativeInterface.getOsType() == SerialNativeInterface.OS_WINDOWS) {
            this.reader = null;
            this.port.addEventListener(this, SerialPort.MASK_RXCHAR);
        }else {
            this.reader = JsscSerialPortManager.instance.addReader(()->{
                try {
                    //Read the bytes, store into queue
                    byte[] buffer = port.readBytes();
                    if(buffer != null) {
                        synchronized (dataStream) {
                            for (int i = 0; i < buffer.length; i++) {
                                this.dataStream.put(buffer[i]);
                            }
                        }
                        if (LOG.isDebugEnabled())
                            LOG.debug(this.port.getPortName() + " recieved: " + StreamUtils.dumpHex(buffer, 0, buffer.length));

                        if (listeners.size() > 0) {
                            //Create a new RX Event
                            final SerialPortProxyEvent upstreamEvent = new SerialPortProxyEvent(Common.timer.currentTimeMillis(), buffer.length);
                            for (final SerialPortProxyEventListener listener : listeners){
                                SerialPortProxyEventTask task = new SerialPortProxyEventTask(listener, upstreamEvent);
                                if(!JsscSerialPortManager.instance.addEvent(task))
                                    LOG.fatal("Serial Port Problem, Listener task queue full, data will be lost!  Increase serial.port.eventQueueSize to avoid this.");
                            }
                        }
                    }
                }
                catch (jssc.SerialPortException e) {
                    LOG.error(e);
                }catch(Exception e) {
                    LOG.error(e);
                }
            }, readPollPeriod, readPollPeriodType);
        }

        if(LOG.isDebugEnabled())
            LOG.debug("Creating Jssc Serial Port Input Stream for: " + serialPort.getPortName());

    }

    @Override
    public int read() throws IOException {
        synchronized (dataStream) {
            try {
                if (dataStream.size() > 0)
                    return dataStream.take() & 0xFF; //Return unsigned byte value by masking off the high order bytes in the returned int
                else
                    return -1;
            }
            catch (InterruptedException e) {
                throw new IOException(e);
            }
        }
    }

    @Override
    public int available() throws IOException {
        synchronized (dataStream) {
            return this.dataStream.size();
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
     * Peek at the head of the stream, do not remove the byte
     *
     * @return
     */
    @Override
    public int peek() {
        return this.dataStream.peek();
    }

    /**
     * Serial Event Executed (Used for Windows Machines)
     */
    @Override
    public void serialEvent(SerialPortEvent event) {
        if (event.isRXCHAR()) {//If data is available
            if (LOG.isDebugEnabled())
                LOG.debug("Serial Receive Event fired.");
            //Read the bytes, store into queue
            try {
                byte[] buffer = this.port.readBytes(event.getEventValue());
                synchronized (dataStream) {
                    for (int i = 0; i < buffer.length; i++) {
                        this.dataStream.put(buffer[i]);
                    }
                }
                if (LOG.isDebugEnabled())
                    LOG.debug("Recieved: " + StreamUtils.dumpHex(buffer, 0, buffer.length));

            }
            catch (Exception e) {
                LOG.error(e);
            }

            if (listeners.size() > 0) {
                //Create a new RX Event
                final SerialPortProxyEvent upstreamEvent = new SerialPortProxyEvent(Common.timer.currentTimeMillis(), event.getEventValue());
                for (final SerialPortProxyEventListener listener : listeners){
                    SerialPortProxyEventTask task = new SerialPortProxyEventTask(listener, upstreamEvent);
                    if(!JsscSerialPortManager.instance.addEvent(task))
                        LOG.fatal("Serial Port Problem, Listener task queue full, data will be lost!  Increase serial.port.eventQueueSize to avoid this.");
                }
            }

        }//end was RX event
        else {
            if (LOG.isDebugEnabled())
                LOG.debug("Non RX Event Type Recieved: " + event.getEventType());
        }
    }
}