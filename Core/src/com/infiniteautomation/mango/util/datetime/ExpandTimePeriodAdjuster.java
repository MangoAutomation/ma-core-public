/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.util.datetime;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjuster;

/**
 * @author Terry Packer
 */
public class ExpandTimePeriodAdjuster implements TemporalAdjuster {

    protected final ZonedDateTime startTime;
    protected final ChronoUnit periodType;
    protected final int periods;

    public ExpandTimePeriodAdjuster(ZonedDateTime startTime, ChronoUnit periodType, int periods) {
        this.startTime = startTime;
        this.periodType = periodType;
        this.periods = periods;
    }

    @Override
    public Temporal adjustInto(Temporal temporal) {
        ZonedDateTime zdt = startTime;
        ZonedDateTime adjustTime = (ZonedDateTime) temporal;
        while (zdt.isBefore(adjustTime)) {
            //We don't need to zero any fields, since we are based on the start date and it would have been
            // adjusted in that manner if appropriate, and none of the time advances should alter a lesser field harmfully.
            zdt = zdt.plus(periods, periodType);
        }
        return zdt;
    }

}
