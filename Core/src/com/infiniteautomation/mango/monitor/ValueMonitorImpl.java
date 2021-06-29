/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.monitor;

import java.util.Objects;

import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * @author Matthew Lohbihler
 * @author Jared Wiltshire
 */
public class ValueMonitorImpl<T> implements ValueMonitor<T> {

    private final String id;
    private final TranslatableMessage name;
    private volatile T value;
    private boolean uploadUsageToStore;

    protected ValueMonitorImpl(String id, TranslatableMessage name, T value, boolean uploadUsageToStore) {
        this.id = Objects.requireNonNull(id);
        this.name = name == null ? new TranslatableMessage("monitor." + id) : name;
        this.value = value;
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
    public void setValue(T value) {
        this.value = value;
    }

    @Override
    public T getValue() {
        return value;
    }

    @Override
    public boolean isUploadToStore() {
        return uploadUsageToStore;
    }
}
