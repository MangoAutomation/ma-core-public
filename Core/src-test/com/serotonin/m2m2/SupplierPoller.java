/*
 * Copyright (C) 2023 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2;

import static org.junit.jupiter.api.Assertions.fail;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Test utility class which polls for values from a supplier.
 *
 * @author Jared Wiltshire
 */
public interface SupplierPoller {

    /**
     * Polls a supplier until it returns true, or fails when 10 seconds elapses.
     */
    default void poll(Supplier<Boolean> supplier) {
        poll(supplier, v -> v);
    }

    /**
     * Polls a supplier until it returns true, or fails when the duration elapses.
     */
    default void poll(Supplier<Boolean> supplier, Duration duration) {
        poll(supplier, v -> v, duration);
    }

    /**
     * Polls a supplier until it returns an instance of the expected class, or fails when 10 seconds elapses.
     */
    default <T, R extends T> R poll(Supplier<T> supplier, Class<R> expectedClass) {
        return poll(supplier, expectedClass, Duration.ofSeconds(10));
    }

    /**
     * Polls a supplier until it returns an instance of the expected class, or fails when the duration elapses.
     */
    default <T, R extends T> R poll(Supplier<T> supplier, Class<R> expectedClass, Duration duration) {
        T v = poll(supplier, expectedClass::isInstance, duration);
        return expectedClass.cast(v);
    }

    /**
     * Polls a supplier until the predicate returns true, or fails when 10 seconds elapses..
     */
    default <T> T poll(Supplier<T> supplier, Predicate<T> predicate) {
        return poll(supplier, predicate, Duration.ofSeconds(10));
    }

    /**
     * Polls a supplier until the predicate returns true, or fails when the duration elapses.
     */
    default <T> T poll(Supplier<T> supplier, Predicate<T> predicate, Duration duration) {
        T value;
        long start = System.nanoTime();
        while (!predicate.test(value = supplier.get())) {
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(10L));
            if (Thread.currentThread().isInterrupted()) {
                fail("Thread was interrupted");
            }
            if (System.nanoTime() - start > duration.toNanos()) {
                fail("Time limit exceeded while polling for value");
            }
        }
        return value;
    }

}
