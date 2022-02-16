/*
 * Copyright (C) 2022 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.view.stats;

/**
 * @author Jared Wiltshire
 */
public class DefaultSeriesValueTime<T> extends DefaultValueTime<T> implements SeriesValueTime<T> {

    private final int seriesId;

    public DefaultSeriesValueTime(int seriesId, long time, T value) {
        super(time, value);
        this.seriesId = seriesId;
    }

    @Override
    public int getSeriesId() {
        return seriesId;
    }
}
