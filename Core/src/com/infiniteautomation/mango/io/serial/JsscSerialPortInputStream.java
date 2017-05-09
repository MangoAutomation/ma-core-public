/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.io.serial;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.io.StreamUtils;
import com.serotonin.m2m2.Common;


/**
 * 
 * Serial port input stream that processes incoming messages is separate threads but in Order.
 * 
 * The max pool size restricts runaway resource usage.  When the pool is full the Recieve events block
 * until the pool is small enough to accept more tasks.
 * 
 * @author Terry Packer
 * 
 */
public class JsscSerialPortInputStream extends SerialPortInputStream implements SerialPortEventListener, SerialPortProxyEventCompleteListener {

    private final Log LOG = LogFactory.getLog(JsscSerialPortInputStream.class);
    protected LinkedBlockingQueue<Byte> dataStream;
    protected SerialPort port;
    protected List<SerialPortProxyEventListener> listeners;

    //Thread Pool for executing listeners in separate threads,
    // when a 
    private BlockingQueue<SerialPortProxyEventTask> listenerTasks;
    private final int maxPoolSize = 20; //Default to 20;
    
    /**
     * @param serialPort
     * @throws SerialPortException
     */
    public JsscSerialPortInputStream(SerialPort serialPort, List<SerialPortProxyEventListener> listeners)
            throws jssc.SerialPortException {
        this.listeners = listeners;
        this.dataStream = new LinkedBlockingQueue<Byte>();

        this.port = serialPort;
        this.port.addEventListener(this, SerialPort.MASK_RXCHAR);
       
        //Setup a bounded Pool that will execute the listener tasks in Order
        this.listenerTasks = new ArrayBlockingQueue<SerialPortProxyEventTask>(this.maxPoolSize);
        
        if(LOG.isDebugEnabled())
        	LOG.debug("Creating Jssc Serial Port Input Stream for: " + serialPort.getPortName());
        
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.io.InputStream#read()
     */
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
        try {
            this.port.removeEventListener(); //Remove the listener
        }
        catch (jssc.SerialPortException e) {
            throw new IOException(e);
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
     * Serial Event Executed
     */
    public void serialEvent(SerialPortEvent event) {
        if (event.isRXCHAR()) {//If data is available
        	if (LOG.isDebugEnabled())
                LOG.debug("Serial Receive Event fired.");
            //Read the bytes, store into queue
            try {
                synchronized (dataStream) {
                	byte[] buffer = this.port.readBytes(event.getEventValue());
                    for (int i = 0; i < buffer.length; i++) {
                        this.dataStream.put(buffer[i]);
                    }
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Recieved: " + StreamUtils.dumpHex(buffer, 0, buffer.length));
                    }
                }
                
            }
            catch (Exception e) {
                LOG.error(e);
            }

            //TODO Add max limit to number of threads we can create until blocking
            // basically use a thread pool here
            if (listeners.size() > 0) {
                //Create a new RX Event
                final SerialPortProxyEvent upstreamEvent = new SerialPortProxyEvent(Common.backgroundProcessing.currentTimeMillis());
                for (final SerialPortProxyEventListener listener : listeners){
                	SerialPortProxyEventTask task = new SerialPortProxyEventTask(listener, upstreamEvent, this);
                	try{
	                	this.listenerTasks.add(task); //Add to queue (wait if queue is full)
	                	task.start();
                	}catch(IllegalStateException e){
                		LOG.warn("Serial Port Problem, Listener task queue full, data will be lost!", e);
                	}
                }
            }

        }//end was RX event
        else {
            if (LOG.isDebugEnabled())
                LOG.debug("Non RX Event Type Recieved: " + event.getEventType());
        }
    }

	@Override
	public void eventComplete(long time, SerialPortProxyEventTask task) {
		this.listenerTasks.remove(task);
	}

}