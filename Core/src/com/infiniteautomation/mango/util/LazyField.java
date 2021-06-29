/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.util;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Provide mechanism for objects to retrieve
 *  relational data from the database when requested and
 *  also to hold any value that is set
 *
 * @author Terry Packer
 */
public class LazyField<T> {

    //TODO Mango 4.0 Make non final and null out after use
    final Supplier<T> delegate;
    volatile T value;
    volatile boolean initialized;

    /**
     * Initialize to null
     */
    public LazyField() {
        this.value = null;
        this.delegate = null;
        this.initialized = true;
    }

    public LazyField(T value) {
        this.value = value;
        this.delegate = null;
        this.initialized = true;
    }

    public LazyField(Supplier<T> delegate) {
        Objects.requireNonNull(delegate);
        this.delegate = delegate;
        this.initialized = false;
    }

    public T get() {
        if (initialized == false) {
            synchronized(this) {
                if (initialized == false) {
                    value = delegate.get();
                    initialized = true;
                }
            }
        }
        return value;
    }

    /**
     * Set the value even if not initialized
     * @param value
     */
    public void set(T value) {
        synchronized(this) {
            this.value = value;
            this.initialized = true;
        }
    }
}
