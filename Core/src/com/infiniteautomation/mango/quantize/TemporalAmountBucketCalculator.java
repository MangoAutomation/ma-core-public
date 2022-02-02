/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.quantize;

import java.time.ZonedDateTime;
import java.time.temporal.TemporalAmount;

public class TemporalAmountBucketCalculator implements BucketCalculator {

    private final ZonedDateTime startTime;
    private final ZonedDateTime endTime;
    private final TemporalAmount amount;

    private ZonedDateTime lastTo;

    public TemporalAmountBucketCalculator(ZonedDateTime startTime, ZonedDateTime endTime, TemporalAmount amount) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.amount = amount;
        this.lastTo = startTime;
    }

    @Override
    public ZonedDateTime getStartTime() {
        return startTime;
    }

    @Override
    public ZonedDateTime getNextPeriodTo() {
        return this.lastTo = lastTo.plus(amount);
    }

    @Override
    public ZonedDateTime getEndTime() {
        return endTime;
    }
}
