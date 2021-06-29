/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.db.query;

import com.serotonin.m2m2.rt.dataImage.PointValueTime;

/**
 * Callback
 * @author Terry Packer
 */
public interface BookendQueryCallback<T extends PointValueTime> extends PVTQueryCallback<T> {

    /**
     * Called with the value at or before the query period start (can be null)
     *
     * If an exception is thrown the query should be aborted
     *
     * @param value
     * @param index of current row
     * @param bookend - true if the value is virtual i.e. added as a point to match the exact query start time for charting
     * @throws QueryCancelledException to abort query
     */
    default void firstValue(T value, int index, boolean bookend) throws QueryCancelledException {
        row(value, index);
    }

    /**
     * Called with the last value before the query period end (can be null)
     *
     * If an exception is thrown the query should be aborted
     *
     * @param value
     * @param index of current row
     * @param bookend - true if the value is virtual i.e. added as a point to match the exact query end time
     * @throws QueryCancelledException to abort query
     */
    default void lastValue(T value, int index, boolean bookend) throws QueryCancelledException {
        row(value, index);
    }


}
