/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.io.serial;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.rt.maint.MangoThreadFactory;

/**
 * @author Terry Packer
 *
 */
public class JsscSerialPortManager {
    
    public static final JsscSerialPortManager instance = new JsscSerialPortManager();
    
    private final Log LOG = LogFactory.getLog(JsscSerialPortManager.class);
    private final ScheduledThreadPoolExecutor readExecutor;
    final BlockingQueue<SerialPortProxyEventTask> eventQueue;
    private Future<?> eventProcessor;
    private volatile boolean running = true;
    
    private JsscSerialPortManager() {
        this.readExecutor = new ScheduledThreadPoolExecutor(1, new MangoThreadFactory("Mango Serial Port Reader", Thread.MAX_PRIORITY));
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
        this.eventProcessor = Common.timer.getExecutorService().submit(() ->{
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
        });
    }
    
    public void terminate() {
        this.readExecutor.shutdown();
        if(this.eventProcessor != null) {
            this.running = false;
            this.eventProcessor.cancel(true);
        }
    }
    
    public void joinTermination() {
        try {
            this.readExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            //No-op
        }
    }
}
