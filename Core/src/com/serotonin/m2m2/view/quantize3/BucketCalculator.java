/**
 * @copyright 2017 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.view.quantize3;

import java.time.ZonedDateTime;

/**
 * A bucket calculator divides a time period into buckets according to the needs of the subclass.
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
