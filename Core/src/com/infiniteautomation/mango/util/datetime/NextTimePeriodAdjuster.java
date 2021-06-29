/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.util.datetime;

import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjuster;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common.TimePeriods;

/**
 * Advance a temporal by the number of time periods of the desired type
 *
 * @author Terry Packer
 */
public class NextTimePeriodAdjuster implements TemporalAdjuster {

    protected final int periodType;
    protected final int periods;
    
    public NextTimePeriodAdjuster(int periodType, int periods) {
        this.periodType = periodType;
        this.periods = periods;
    }
    
    /* (non-Javadoc)
     * @see java.time.temporal.TemporalAdjuster#adjustInto(java.time.temporal.Temporal)
     */
    @Override
    public Temporal adjustInto(Temporal temporal) {
        switch (periodType) {
            case TimePeriods.MILLISECONDS:
                return temporal.plus(periods, ChronoUnit.MILLIS);
            case TimePeriods.SECONDS:
                return temporal.plus(periods, ChronoUnit.SECONDS);
            case TimePeriods.MINUTES:
                return temporal.plus(periods, ChronoUnit.MINUTES);
            case TimePeriods.HOURS:
                return temporal.plus(periods, ChronoUnit.HOURS);
            case TimePeriods.DAYS:
                return temporal.plus(periods, ChronoUnit.DAYS);
            case TimePeriods.WEEKS:
                return temporal.plus(periods, ChronoUnit.WEEKS);
            case TimePeriods.MONTHS:
                return temporal.plus(periods, ChronoUnit.MONTHS);
            case TimePeriods.YEARS:
                return temporal.plus(periods, ChronoUnit.YEARS);
            default:
                throw new ShouldNeverHappenException("Unsupported time period: " + periodType);
            }
    }
}
