/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.quantize;

import java.time.ZonedDateTime;

import com.infiniteautomation.mango.util.datetime.NextTimePeriodAdjuster;

/**
 * Time period bucket calculator divides the given time range into the given periods. The intervals that are produces
 * may not be equal in duration. For example, if the time period is one day, the duration may be 23, 24, or 25 hours
 * depending on whether the day is a daylight savings change day or not.
 * 
 * @author Terry Packer
 */
public class TimePeriodBucketCalculator implements BucketCalculator {
    
    private final ZonedDateTime startTime;
    private final ZonedDateTime endTime;
    private final NextTimePeriodAdjuster adjuster;
    
    private ZonedDateTime lastTo;

    public TimePeriodBucketCalculator(ZonedDateTime startTime, ZonedDateTime endTime, int periodType, int periods) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.adjuster = new NextTimePeriodAdjuster(periodType, periods);

        lastTo = startTime;
    }

    @Override
    public ZonedDateTime getStartTime() {
        return startTime;
    }

    @Override
    public ZonedDateTime getNextPeriodTo() {
        lastTo = (ZonedDateTime) adjuster.adjustInto(lastTo);
        return lastTo;
    }

    @Override
    public ZonedDateTime getEndTime() {
        return endTime;
    }
}
