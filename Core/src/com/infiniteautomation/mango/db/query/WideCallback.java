/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.db.query;

import java.util.function.Consumer;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Callback for TSDB operations, including ability to be notified of first and last values.
 * @author Terry Packer
 * @author Jared Wiltshire
 */
@FunctionalInterface
public interface WideCallback<T> extends Consumer<T> {

    default void firstValue(@Nullable T value) {
        firstValue(value, false);
    }

    default void lastValue(@Nullable T value) {
        lastValue(value, false);
    }

    /**
     * Called with the value at or before the query period start (can be null)
     *
     * If an exception is thrown the query should be aborted
     *
     * @param value the point value
     * @param bookend - true if the value is virtual i.e. added as a point to match the exact query start time for charting
     */
    default void firstValue(@Nullable T value, boolean bookend) {
        accept(value);
    }

    /**
     * Called with the last value before the query period end (can be null)
     *
     * If an exception is thrown the query should be aborted
     *
     * @param value the point value
     * @param bookend - true if the value is virtual i.e. added as a point to match the exact query end time
     */
    default void lastValue(@Nullable T value, boolean bookend) {
        accept(value);
    }

}
