/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.db.query;

import com.serotonin.m2m2.rt.dataImage.PointValueTime;

/**
 * Base interface for point value time query callbacks
 *
 * @author Terry Packer
 */
@FunctionalInterface
public interface PVTQueryCallback<T extends PointValueTime> {

    /**
     * Values within the query
     *
     * If an exception is thrown the query should be aborted
     *
     * @param value
     * @param index
     * @throws QueryCancelledException to abort query
     */
    void row(T value, int index) throws QueryCancelledException;
}
