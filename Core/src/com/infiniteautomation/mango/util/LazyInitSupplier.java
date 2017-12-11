/*
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.util;

import java.util.function.Supplier;

/**
 * Thread safe lazy initializer that can be reset.
 * Uses double checked locking.
 * 
 * @author Jared Wiltshire
 */
public class LazyInitSupplier<T> extends LazyInitializer<T> implements Supplier<T> {
    
    final Supplier<T> delegate;
    volatile T value;
    
    public LazyInitSupplier(Supplier<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public T get() {
        return this.get(this.delegate);
    }

}
