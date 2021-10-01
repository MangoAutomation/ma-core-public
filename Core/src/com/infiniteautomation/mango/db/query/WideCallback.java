/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.db.query;

import java.util.function.Consumer;

/**
 * Callback for TSDB operations, including ability to be notified of first and last values.
 * @author Terry Packer
 * @author Jared Wiltshire
 */
@FunctionalInterface
public interface WideCallback<T> extends Consumer<T> {

    default void firstValue(T value) {
        firstValue(value, false);
    }

    default void lastValue(T value) {
        lastValue(value, false);
    }

    /**
     * Called with the value at or before the query period start (can be null)
     *
     * If an exception is thrown the query should be aborted
     *
     * @param value
     * @param bookend - true if the value is virtual i.e. added as a point to match the exact query start time for charting
     * @throws QueryCancelledException to abort query
     */
    default void firstValue(T value, boolean bookend) {
        accept(value);
    }

    /**
     * Called with the last value before the query period end (can be null)
     *
     * If an exception is thrown the query should be aborted
     *
     * @param value
     * @param bookend - true if the value is virtual i.e. added as a point to match the exact query end time
     * @throws QueryCancelledException to abort query
     */
    default void lastValue(T value, boolean bookend) {
        accept(value);
    }


}
