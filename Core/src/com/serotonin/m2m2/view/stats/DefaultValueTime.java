/*
 * Copyright (C) 2022 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.view.stats;

/**
 * @author Jared Wiltshire
 */
public class DefaultValueTime<T> implements IValueTime<T> {

    private final long time;
    private final T value;

    public DefaultValueTime(long time, T value) {
        this.time = time;
        this.value = value;
    }

    @Override
    public long getTime() {
        return time;
    }

    @Override
    public T getValue() {
        return value;
    }

}
