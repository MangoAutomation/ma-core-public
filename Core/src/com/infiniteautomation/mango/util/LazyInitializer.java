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
public class LazyInitializer<T> {

    volatile T value;

    /**
     * Get the already initialized value or use the provided supplier to initialize it.
     * 
     * @param supplier
     * @return
     */
    public T get(final Supplier<T> supplier) {
        T result = value;
        if (result == null) {
            synchronized(this) {
                result = value;
                if (result == null) {
                    value = result = supplier.get();
                }
            }
        }
        return result;
    }
    
    /**
     * Reset the value so that next time get() is called it re-initializes the value.
     */
    public void reset() {
        value = null;
    }

}
