/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.monitor;

import java.util.function.Supplier;

import com.serotonin.m2m2.i18n.TranslatableMessage;

public class ReadThroughMonitor<T> implements ValueMonitor<T> {

    private final String id;
    private final TranslatableMessage name;
    private final boolean uploadToStore;
    private final Supplier<T> supplier;

    protected ReadThroughMonitor(String id, TranslatableMessage name, Supplier<T> supplier, boolean uploadToStore) {
        this.id = id;
        this.name = name;
        this.uploadToStore = uploadToStore;
        this.supplier = supplier;
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
        throw new UnsupportedOperationException();
    }

    @Override
    public T getValue() {
        return supplier.get();
    }

    @Override
    public boolean isUploadToStore() {
        return uploadToStore;
    }
}
