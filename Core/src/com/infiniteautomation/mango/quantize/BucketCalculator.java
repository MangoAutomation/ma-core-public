/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.quantize;

import java.time.ZonedDateTime;

/**
 * A bucket calculator divides a time period into buckets according to the needs of the subclass.
 * 
 * startTime <= time < endTime
 * 
 * @author Terry Packer
 */
public interface BucketCalculator {
    /**
     * The time from which the range is divided.
     * 
     * @return
     */
    ZonedDateTime getStartTime();

    /**
     * The next period end time. The value returned increments with each call.
     * 
     * @return
     */
    ZonedDateTime getNextPeriodTo();

    /**
     * The time to which the range is divided.
     * 
     * @return
     */
    ZonedDateTime getEndTime();
}
