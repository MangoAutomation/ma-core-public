/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.quantize;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/**
 * This bucket calculator divides the given time range into a given number of buckets of equal duration.
 * 
 * @author Terry Packer
 */
public class BucketsBucketCalculator implements BucketCalculator {
    private final ZonedDateTime startTime;
    private final ZonedDateTime endTime;
    private final int buckets;
    private final long duration;

    private int periodCounter;

    public BucketsBucketCalculator(ZonedDateTime startTime, ZonedDateTime endTime, int buckets) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.buckets = buckets;
        Duration d = Duration.between(startTime, endTime);
        this.duration = d.toMillis();
    }

    @Override
    public ZonedDateTime getStartTime() {
        return startTime;
    }

    @Override
    public ZonedDateTime getNextPeriodTo() {
        double interval = ((double) duration) / buckets * ++periodCounter;
        return startTime.plus((long) (interval + 0.5), ChronoUnit.MILLIS);
    }

    @Override
    public ZonedDateTime getEndTime() {
        return endTime;
    }
}
