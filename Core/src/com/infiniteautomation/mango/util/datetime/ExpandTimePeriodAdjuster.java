/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.util.datetime;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjuster;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common.TimePeriods;

/**
 *
 * @author Terry Packer
 */
public class ExpandTimePeriodAdjuster implements TemporalAdjuster {

    protected final ZonedDateTime startTime;
    protected final int periodType;
    protected final int periods;
    
    public ExpandTimePeriodAdjuster(ZonedDateTime startTime, int periodType, int periods) {
        this.startTime = startTime;
        this.periodType = periodType;
        this.periods = periods;
    }

    /* (non-Javadoc)
     * @see java.time.temporal.TemporalAdjuster#adjustInto(java.time.temporal.Temporal)
     */
    @Override
    public Temporal adjustInto(Temporal temporal) {
        ZonedDateTime zdt = startTime;
        ZonedDateTime adjustTime = (ZonedDateTime) temporal;
        while(zdt.isBefore(adjustTime)) {
            //We don't need to zero any fields, since we are based on the start date and it would have been
            // adjusted in that manner if appropriate, and none of the time advances should alter a lesser field harmfully.
            switch(periodType) {
                case TimePeriods.MILLISECONDS:
                    zdt = zdt.plus(periods, ChronoUnit.MILLIS);
                    break;
                case TimePeriods.SECONDS:
                    zdt = zdt.plus(periods, ChronoUnit.SECONDS);
                    break;
                case TimePeriods.MINUTES:
                    zdt = zdt.plus(periods, ChronoUnit.MINUTES);
                    break;
                case TimePeriods.HOURS:
                    zdt = zdt.plus(periods, ChronoUnit.HOURS);
                    break;
                case TimePeriods.DAYS:
                    zdt = zdt.plus(periods, ChronoUnit.DAYS);
                    break;
                case TimePeriods.WEEKS:
                  //Don't set the DoW since this may be governed by last year, and shouldn't change by adding weeks
                    zdt = zdt.plus(periods, ChronoUnit.WEEKS);
                    break;
                case TimePeriods.MONTHS:
                    zdt = zdt.plus(periods, ChronoUnit.MONTHS);
                    break;
                case TimePeriods.YEARS:
                    zdt = zdt.plus(periods, ChronoUnit.YEARS);
                    break;
                default:
                    throw new ShouldNeverHappenException("Unsupported time period: " + periodType);
            }
        }
        return zdt;
    }

}
