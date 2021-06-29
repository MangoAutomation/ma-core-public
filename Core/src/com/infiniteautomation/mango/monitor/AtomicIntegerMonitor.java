/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.monitor;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * Threadsafe Integer Monitor
 * @author Terry Packer
 * @author Jared Wiltshire
 */
public class AtomicIntegerMonitor implements ValueMonitor<Integer> {

    private final String id;
    private final TranslatableMessage name;
    private final AtomicInteger value;
    private boolean uploadUsageToStore;

    protected AtomicIntegerMonitor(String id, TranslatableMessage name, Integer value, boolean uploadUsageToStore) {
        this.id = Objects.requireNonNull(id);
        this.name = name == null ? new TranslatableMessage("monitor." + id) : name;
        this.value = new AtomicInteger(value);
        this.uploadUsageToStore = uploadUsageToStore;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public TranslatableMessage getName() {
        return name;
    }

    @Override
    public void setValue(Integer value) {
        this.value.set(value);
    }

    @Override
    public Integer getValue() {
        return this.value.get();
    }

    @Override
    public boolean isUploadToStore() {
        return uploadUsageToStore;
    }

    public void addValue(int value) {
        this.value.addAndGet(value);
    }

    public void setValueIfGreater(int value) {
        this.value.updateAndGet(current -> value > current ? value : current);
    }

    public void increment() {
        this.value.incrementAndGet();
    }

    public void decrement() {
        this.value.decrementAndGet();
    }
}
