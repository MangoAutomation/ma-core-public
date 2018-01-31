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
public class AtomicIntegerMonitor extends ValueMonitor<Integer> {

	private final AtomicInteger value;
	
	public AtomicIntegerMonitor(String id, TranslatableMessage name, ValueMonitorOwner owner) {
        this(id, name, owner, 0);
    }

    public AtomicIntegerMonitor(String id, TranslatableMessage name, ValueMonitorOwner owner, int initialValue) {
        super(id, name, owner);
        value = new AtomicInteger(initialValue);
    }

    @Override
    public Integer getValue() {
        return value.get();
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
    
    @Override
    public int intValue() {
        return value.get();
    }
}
