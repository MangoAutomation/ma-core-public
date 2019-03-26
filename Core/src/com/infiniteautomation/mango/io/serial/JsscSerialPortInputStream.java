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
 * Serial port input stream that processes incoming messages is separate threads but in Order.
 * 
 * The max pool size restricts runaway resource usage.  When the pool is full the Recieve events block
 * until the pool is small enough to accept more tasks.
 * 
 * @author Terry Packer
 * 
 */
public class JsscSerialPortInputStream extends SerialPortInputStream implements SerialPortEventListener {

    private final Log LOG = LogFactory.getLog(JsscSerialPortInputStream.class);
    protected final LinkedBlockingQueue<Byte> dataStream;
    protected final SerialPort port;
    protected final List<SerialPortProxyEventListener> listeners;
    protected final SerialPortEventProcessorThread eventProcessor;
    
    protected final SerialPortReadThread readThread;
    //TODO Expose these as env properties?
    protected final int readThreadSleepMillis = 0;
    protected final int readThreadSleepNanos = 100;
    
    //Tasks to be run for listeners
    protected final BlockingQueue<SerialPortProxyEventTask> listenerTasks;
    
    /**
     * @param serialPort
     * @throws SerialPortException
     */
    public JsscSerialPortInputStream(SerialPort serialPort, List<SerialPortProxyEventListener> listeners)
            throws jssc.SerialPortException {
        this.listeners = listeners;
        this.dataStream = new LinkedBlockingQueue<Byte>();

        this.port = serialPort;
        
        //This is ok on windows, linux we want to poll on our own
        if(SerialNativeInterface.getOsType() == SerialNativeInterface.OS_WINDOWS) {
            this.readThread = null;
            this.port.addEventListener(this, SerialPort.MASK_RXCHAR);
        }else {
            this.readThread = new SerialPortReadThread(port.getPortName());
        }
       
        //Setup a bounded Pool that will execute the listener tasks in Order
        this.listenerTasks = new ArrayBlockingQueue<SerialPortProxyEventTask>(10000);
        
        this.eventProcessor = new SerialPortEventProcessorThread(port.getPortName());
        
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
            if(this.readThread != null) {
                try {
                    this.readThread.terminate();
                    this.readThread.joinTermination();
                }catch(Exception e) {
                    LOG.error("Serial port read thread " + this.readThread.getName() + "failed to stop", e);
                }
            }
        }
        try {
            this.eventProcessor.terminate();
            this.eventProcessor.joinTermination();
        }catch(Exception e) {
            LOG.error("Serial port event processor thread " + this.eventProcessor.getName() + " failed to stop", e);
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

            if (listeners.size() > 0) {
                //Create a new RX Event
                final SerialPortProxyEvent upstreamEvent = new SerialPortProxyEvent(Common.timer.currentTimeMillis());
                for (final SerialPortProxyEventListener listener : listeners){
                	SerialPortProxyEventTask task = new SerialPortProxyEventTask(listener, upstreamEvent);
                	try{
	                	this.listenerTasks.offer(task);
                	}catch(IllegalStateException e){
                		LOG.fatal("Serial Port Problem, Listener task queue full, data will be lost!", e);
                	}
                }
            }

        }//end was RX event
        else {
            if (LOG.isDebugEnabled())
                LOG.debug("Non RX Event Type Recieved: " + event.getEventType());
        }
    }

    /**
     * Process the events by running their listener tasks
     * @author Terry Packer
     */
	private class SerialPortEventProcessorThread extends Thread {
	    
	    private volatile boolean running = true;
	    private volatile boolean stopped = false;
	    
	    public SerialPortEventProcessorThread(String portName) {
	        super("jssc-serial-port-event-processor[" + portName + "]");
	        this.start();
	    }
	    
	    @Override
	    public void run() {
	        while(running) {
	            try {
	                //Block until I have work to do
	                SerialPortProxyEventTask t = listenerTasks.take();
	                t.run();
                } catch (InterruptedException e) {
                    //Don't care, this will very likely happen on terminate
                }
	        }
	        stopped = true;
	    }
	    
	    public void terminate() {
	        this.running = false;
	        this.interrupt();
	    }
	    
	    public void joinTermination() {
	        while(!stopped) {
	            try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    
                }
	        }
	    }
	}
	
    /**
     * Thread to poll for new input
     * 
     * @author Terry Packer
     *
     */
    private class SerialPortReadThread extends Thread {

        private volatile boolean running = true;
        private volatile boolean stopped = false;

        public SerialPortReadThread(String portName) {
            super("jssc-serial-port-reader[" + portName + "]");
            this.start();
        }

        @Override
        public void run() {
            int available = 0;
            while(running) {
                try {
                    available = port.getInputBufferBytesCount();
                    if(available > 0) {
                        SerialPortEvent event = new SerialPortEvent(port.getPortName(), SerialPortEvent.RXCHAR, available);
                        serialEvent(event);
                    }
                    Thread.sleep(readThreadSleepMillis, readThreadSleepNanos);
                }
                catch (jssc.SerialPortException e) {
                    LOG.error(e);
                }catch(InterruptedException e) {
                    //Don't care, this could happen on terminate
                }
            }
            stopped = true;
        }
        
        public void terminate() {
            this.running = false;
            this.interrupt();
        }
        
        public void joinTermination() {
            while(!stopped) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    
                }
            }
        }
	}
}