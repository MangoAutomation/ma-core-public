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
public class LazyInitializer<T> implements Supplier<T> {
    
    final Supplier<T> delegate;
    volatile T value;
    
    public LazyInitializer(Supplier<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public T get() {
        T result = value;
        if (result == null) {
            synchronized(this) {
                result = value;
                if (result == null) {
                    value = result = this.delegate.get();
                }
            }
        }
        return result;
    }
    
    public void reset() {
        value = null;
    }

}
