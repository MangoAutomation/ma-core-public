/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.monitor;

import java.util.Objects;
import java.util.function.Function;

import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * @author Jared Wiltshire
 */
public class PollableMonitorImpl<T> implements PollableMonitor<T> {

    private final String id;
    private final TranslatableMessage name;
    private volatile T value;
    private boolean uploadToStore;
    private final Function<Long, T> function;

    protected PollableMonitorImpl(String id, TranslatableMessage name, Function<Long, T> function, boolean uploadToStore) {
        this.id = Objects.requireNonNull(id);
        this.name = name == null ? new TranslatableMessage("monitor." + id) : name;
        this.function = Objects.requireNonNull(function);
        this.uploadToStore = uploadToStore;
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
    public void setValue(T value) {
        this.value = value;
    }

    @Override
    public T getValue() {
        return value;
    }

    @Override
    public boolean isUploadToStore() {
        return uploadToStore;
    }

    @Override
    public T poll(long ts) {
        return (this.value = this.function.apply(ts));
    }
}
