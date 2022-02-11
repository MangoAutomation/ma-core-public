/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.util.datetime;

import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjuster;

import com.serotonin.ShouldNeverHappenException;

/**
 * Truncate the Temporal to the desired resolution
 *
 * @author Terry Packer
 */
public class TruncateTimePeriodAdjuster implements TemporalAdjuster {

    protected final ChronoUnit periodType;
    protected final int periods;
    
    public TruncateTimePeriodAdjuster(ChronoUnit periodType, int periods) {
        this.periodType = periodType;
        this.periods = periods;
    }

    @Override
    public Temporal adjustInto(Temporal temporal) {
        //Days, weeks, and months have a -1 because their value ranges start at 1 not 0
        switch (periodType) {
            case MILLIS:
                if(periods > 1)
                    temporal = temporal.minus(temporal.get(ChronoField.MILLI_OF_SECOND) % periods, ChronoUnit.MILLIS);
                break;
            case SECONDS:
                if(periods > 1)
                    temporal = temporal.minus(temporal.get(ChronoField.SECOND_OF_MINUTE) % periods, ChronoUnit.SECONDS);
                temporal = temporal.with(ChronoField.MILLI_OF_SECOND, 0);
                break;
            case MINUTES:
                if(periods > 1)
                    temporal = temporal.minus(temporal.get(ChronoField.MINUTE_OF_HOUR) % periods, ChronoUnit.MINUTES);
                temporal = temporal.with(ChronoField.SECOND_OF_MINUTE, 0);
                temporal = temporal.with(ChronoField.MILLI_OF_SECOND, 0);
                break;
            case HOURS:
                if(periods > 1)
                    temporal = temporal.with(ChronoField.HOUR_OF_DAY, temporal.get(ChronoField.HOUR_OF_DAY) - temporal.get(ChronoField.HOUR_OF_DAY) % periods);
                temporal = temporal.with(ChronoField.MINUTE_OF_HOUR, 0);
                temporal = temporal.with(ChronoField.SECOND_OF_MINUTE, 0);
                temporal = temporal.with(ChronoField.MILLI_OF_SECOND, 0);
                break;
            case DAYS:
                //TODO See Aligned Day of Week In Year Chrono Field for other options of this 
                if(periods > 1)
                    temporal = temporal.minus((temporal.get(ChronoField.DAY_OF_YEAR)-1) % periods, ChronoUnit.DAYS);
                temporal = temporal.with(ChronoField.HOUR_OF_DAY, 0);
                temporal = temporal.with(ChronoField.MINUTE_OF_HOUR, 0);
                temporal = temporal.with(ChronoField.SECOND_OF_MINUTE, 0);
                temporal = temporal.with(ChronoField.MILLI_OF_SECOND, 0);
                break;
            case WEEKS:
                if(periods > 1)
                    temporal = temporal.minus((temporal.get(ChronoField.ALIGNED_WEEK_OF_YEAR)-1) % periods, ChronoUnit.WEEKS);
                temporal = temporal.with(ChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR, 1);
                temporal = temporal.with(ChronoField.HOUR_OF_DAY, 0);
                temporal = temporal.with(ChronoField.MINUTE_OF_HOUR, 0);
                temporal = temporal.with(ChronoField.SECOND_OF_MINUTE, 0);
                temporal = temporal.with(ChronoField.MILLI_OF_SECOND, 0);
                break;
            case MONTHS:
                if(periods > 1)
                    temporal = temporal.minus((temporal.get(ChronoField.MONTH_OF_YEAR)-1) % periods, ChronoUnit.MONTHS);
                temporal = temporal.with(ChronoField.DAY_OF_MONTH, 1);
                temporal = temporal.with(ChronoField.HOUR_OF_DAY, 0);
                temporal = temporal.with(ChronoField.MINUTE_OF_HOUR, 0);
                temporal = temporal.with(ChronoField.SECOND_OF_MINUTE, 0);
                temporal = temporal.with(ChronoField.MILLI_OF_SECOND, 0);
                break;
            case YEARS:
                if(periods > 1) {
                    //Compute year of century for compatiblity with DateUtils
                    int yearOfCentury = temporal.get(ChronoField.YEAR) % 100;
                    temporal = temporal.minus(yearOfCentury % periods, ChronoUnit.YEARS);
                }
                temporal = temporal.with(ChronoField.DAY_OF_YEAR, 1);
                temporal = temporal.with(ChronoField.HOUR_OF_DAY, 0);
                temporal = temporal.with(ChronoField.MINUTE_OF_HOUR, 0);
                temporal = temporal.with(ChronoField.SECOND_OF_MINUTE, 0);
                temporal = temporal.with(ChronoField.MILLI_OF_SECOND, 0);
                break;
            default:
                throw new ShouldNeverHappenException("Unsupported time period: " + periodType);
            }
        
        return temporal;
    }
}
