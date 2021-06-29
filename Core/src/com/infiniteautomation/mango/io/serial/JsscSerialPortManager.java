/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.io.serial;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.rt.maint.MangoThreadFactory;

/**
 * 
 * Container to manage the ThreadPoolExecutor that reads from serial ports and the event processor that executes the read events.
 * 
 * @author Terry Packer
 *
 */
public class JsscSerialPortManager {
    
    public static final JsscSerialPortManager instance = new JsscSerialPortManager();
    
    private final Log LOG = LogFactory.getLog(JsscSerialPortManager.class);
    private final ScheduledThreadPoolExecutor readExecutor;
    final BlockingQueue<SerialPortProxyEventTask> eventQueue;
    private Thread eventProcessor;
    private volatile boolean running = true;
    private boolean initialized = false;
    
    private JsscSerialPortManager() {
        this.readExecutor = new ScheduledThreadPoolExecutor(1, new MangoThreadFactory("Mango Serial Port Reader", Thread.MAX_PRIORITY, Common.getModuleClassLoader()));
        this.eventQueue = new ArrayBlockingQueue<SerialPortProxyEventTask>(Common.envProps.getInt("serial.port.eventQueueSize", 10000));
    }
    
    /**
     * Add a read task
     * @param reader
     * @param period
     * @param unit
     * @return
     */
    public ScheduledFuture<?> addReader(Runnable reader, long period, TimeUnit unit) {
        return this.readExecutor.scheduleAtFixedRate(reader, 0, period, unit);
    }
    
    /**
     * Add a task to be processed
     * @param task
     * @return
     */
    public boolean addEvent(SerialPortProxyEventTask task) {
        return this.eventQueue.offer(task);
    }
    
    public void initialize() {
        if(initialized) {
            throw new ShouldNeverHappenException("Already initialized");
        }
        this.eventProcessor = new Thread("Mango Serial Port Event Processor") { 
            public void run() {
                while(running) {
                    try {
                        //Block until I have work to do
                        SerialPortProxyEventTask t = eventQueue.take();
                        t.run();
                    } catch (InterruptedException e) {
                        //Don't care, this will very likely happen on terminate
                    }catch(Exception e) {
                        LOG.error("Serial Port Event Task Failed", e);
                    }
                }
                LOG.info("Mango Serial Port Event Processor Terminated");
            }
        };
        this.eventProcessor.setPriority(Thread.MAX_PRIORITY);
        this.eventProcessor.start();
        this.initialized = true;
    }
    
    public void terminate() {
        this.readExecutor.shutdown();
        if(this.eventProcessor != null) {
            this.running = false;
            this.eventProcessor.interrupt();
        }
    }
    
    public void joinTermination() {
        try {
            this.readExecutor.awaitTermination(5, TimeUnit.SECONDS);
            this.initialized = false;
        } catch (InterruptedException e) {
            //No-op
        }
    }
}
