/**
 * @copyright 2017 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.util.datetime;

import java.time.DayOfWeek;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjuster;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common.TimePeriods;

/**
 * Truncate the Temporal to the desired resolution
 *
 * @author Terry Packer
 */
public class TruncateTimePeriodAdjuster implements TemporalAdjuster {

    protected final int periodType;
    protected final int periods;
    
    public TruncateTimePeriodAdjuster(int periodType, int periods) {
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
                if(periods > 1)
                    temporal = temporal.minus(temporal.get(ChronoField.MILLI_OF_SECOND) % periods, ChronoUnit.MILLIS);
                break;
            case TimePeriods.SECONDS:
                if(periods > 1)
                    temporal = temporal.minus(temporal.get(ChronoField.SECOND_OF_MINUTE) % periods, ChronoUnit.SECONDS);
                temporal = temporal.with(ChronoField.MILLI_OF_SECOND, 0);
                break;
            case TimePeriods.MINUTES:
                if(periods > 1)
                    temporal = temporal.minus(temporal.get(ChronoField.MINUTE_OF_HOUR) % periods, ChronoUnit.MINUTES);
                temporal = temporal.with(ChronoField.SECOND_OF_MINUTE, 0);
                temporal = temporal.with(ChronoField.MILLI_OF_SECOND, 0);
                break;
            case TimePeriods.HOURS:
                if(periods > 1)
                    temporal = temporal.minus(temporal.get(ChronoField.HOUR_OF_DAY) % periods, ChronoUnit.HOURS);
                temporal = temporal.with(ChronoField.MINUTE_OF_HOUR, 0);
                temporal = temporal.with(ChronoField.SECOND_OF_MINUTE, 0);
                temporal = temporal.with(ChronoField.MILLI_OF_SECOND, 0);
                break;
            case TimePeriods.DAYS:
                //TODO See Aligned Day of Week In Year Chrono Field for other options of this 
                if(periods > 1)
                    temporal = temporal.minus(temporal.get(ChronoField.DAY_OF_YEAR) % periods, ChronoUnit.DAYS);
                temporal = temporal.with(ChronoField.HOUR_OF_DAY, 0);
                temporal = temporal.with(ChronoField.MINUTE_OF_HOUR, 0);
                temporal = temporal.with(ChronoField.SECOND_OF_MINUTE, 0);
                temporal = temporal.with(ChronoField.MILLI_OF_SECOND, 0);
                break;
            case TimePeriods.WEEKS:
                if(periods > 1)
                    temporal = temporal.minus(temporal.get(ChronoField.ALIGNED_WEEK_OF_YEAR) % periods, ChronoUnit.WEEKS);
                temporal = temporal.with(ChronoField.DAY_OF_WEEK, DayOfWeek.MONDAY.getValue());
                temporal = temporal.with(ChronoField.HOUR_OF_DAY, 0);
                temporal = temporal.with(ChronoField.MINUTE_OF_HOUR, 0);
                temporal = temporal.with(ChronoField.SECOND_OF_MINUTE, 0);
                temporal = temporal.with(ChronoField.MILLI_OF_SECOND, 0);
                break;
            case TimePeriods.MONTHS:
                if(periods > 1)
                    temporal = temporal.minus(temporal.get(ChronoField.MONTH_OF_YEAR) % periods, ChronoUnit.MONTHS);
                temporal = temporal.with(ChronoField.DAY_OF_MONTH, 1);
                temporal = temporal.with(ChronoField.HOUR_OF_DAY, 0);
                temporal = temporal.with(ChronoField.MINUTE_OF_HOUR, 0);
                temporal = temporal.with(ChronoField.SECOND_OF_MINUTE, 0);
                temporal = temporal.with(ChronoField.MILLI_OF_SECOND, 0);
            case TimePeriods.YEARS:
                if(periods > 1) {
                    //Compute year of century for compatiblity with DateUtils
                    int yearOfCentury = temporal.get(ChronoField.YEAR) / 100;
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
