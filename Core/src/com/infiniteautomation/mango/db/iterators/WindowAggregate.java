/*
 * Copyright (C) 2022 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.db.iterators;

import java.time.ZonedDateTime;
import java.util.DoubleSummaryStatistics;

import com.serotonin.m2m2.rt.dataImage.PointValueTime;

/**
 * @author Jared Wiltshire
 */
public class WindowAggregate {
    private final ZonedDateTime start;
    private final ZonedDateTime end;
    private final long startTimestamp;
    private final long endTimestamp;
    private final DoubleSummaryStatistics statistics = new DoubleSummaryStatistics();

    public WindowAggregate(ZonedDateTime start, ZonedDateTime end) {
        this.start = start;
        this.end = end;
        this.startTimestamp = start.toInstant().toEpochMilli();
        this.endTimestamp = end.toInstant().toEpochMilli();
    }

    public ZonedDateTime getStart() {
        return start;
    }

    public ZonedDateTime getEnd() {
        return end;
    }

    public long getStartTimestamp() {
        return startTimestamp;
    }

    public long getEndTimestamp() {
        return endTimestamp;
    }

    public DoubleSummaryStatistics getStatistics() {
        return statistics;
    }

    public boolean accumulate(PointValueTime value) {
        long time = value.getTime();
        if (time >= startTimestamp && time < endTimestamp) {
            statistics.accept(value.getDoubleValue());
            return true;
        }
        return false;
    }
}
