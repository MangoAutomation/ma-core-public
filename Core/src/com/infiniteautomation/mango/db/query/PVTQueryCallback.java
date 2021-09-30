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
     * If an exception is thrown the query will be aborted.
     */
    void row(T value);

}
