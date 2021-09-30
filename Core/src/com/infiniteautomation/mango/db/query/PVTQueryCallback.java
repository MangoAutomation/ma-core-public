/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.db.query;

import java.util.function.Consumer;

import com.serotonin.m2m2.rt.dataImage.PointValueTime;

/**
 * Base interface for point value time query callbacks
 *
 * @author Terry Packer
 */
@FunctionalInterface
public interface PVTQueryCallback<T extends PointValueTime> extends Consumer<T> {

    /**
     * If an exception is thrown the query will be aborted.
     */
    void row(T value);

    @Override
    default void accept(T t) {
        row(t);
    }
}
