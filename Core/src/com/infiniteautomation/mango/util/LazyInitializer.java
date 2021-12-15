/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.util;

import java.util.Objects;
import java.util.Optional;
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
     * @param supplier must not return null
     */
    public T get(final Supplier<T> supplier) {
        Objects.requireNonNull(supplier);

        T result = value;
        if (result == null) {
            synchronized(this) {
                result = value;
                if (result == null) {
                    value = result = Objects.requireNonNull(supplier.get());
                }
            }
        }
        return result;
    }

    /**
     * @return the value if it is already initialized, otherwise null
     */
    public Optional<T> getIfInitialized() {
        return Optional.ofNullable(this.value);
    }

    /**
     * Reset the value so that next time get() is called it re-initializes the value.
     */
    public void reset() {
        value = null;
    }

}
