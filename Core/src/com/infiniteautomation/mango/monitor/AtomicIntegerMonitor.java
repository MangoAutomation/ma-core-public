/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 *
 */
package com.infiniteautomation.mango.monitor;

import java.util.concurrent.atomic.AtomicInteger;

import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * Threadsafe Integer Monitor
 * @author Terry Packer
 */
public class AtomicIntegerMonitor extends ValueMonitor<AtomicInteger> {

    /**
     * Create monitor with initial value of 0
     * @param id
     * @param name
     * @param owner
     */
	public AtomicIntegerMonitor(String id, TranslatableMessage name, ValueMonitorOwner owner) {
        super(id, name, owner, new AtomicInteger(0));
    }

    public AtomicIntegerMonitor(String id, TranslatableMessage name, ValueMonitorOwner owner,
            boolean uploadUsageToStore) {
        super(id, name, owner, new AtomicInteger(0), uploadUsageToStore);
    }
	
    public AtomicIntegerMonitor(String id, TranslatableMessage name, ValueMonitorOwner owner, AtomicInteger initialValue) {
        super(id, name, owner, initialValue);
    }

    public void setValue(int value) {
        this.value.set(value);
    }

    public void addValue(int value) {
        this.value.addAndGet(value);
    }

    public void setValueIfGreater(int value) {
        this.value.updateAndGet(current -> value > current ? value : current);
    }

    public void increment(){
    	this.value.incrementAndGet();
    }
    
    public void decrement(){
    	this.value.decrementAndGet();
    }
}
