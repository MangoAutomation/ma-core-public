/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.util.datetime;

import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjuster;

/**
 * Advance a temporal by the number of time periods of the desired type
 *
 * @author Terry Packer
 */
public class NextTimePeriodAdjuster implements TemporalAdjuster {

    protected final ChronoUnit periodType;
    protected final int periods;

    public NextTimePeriodAdjuster(ChronoUnit periodType, int periods) {
        this.periodType = periodType;
        this.periods = periods;
    }

    @Override
    public Temporal adjustInto(Temporal temporal) {
        return temporal.plus(periods, periodType);
    }
}
