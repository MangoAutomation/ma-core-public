/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.util.datetime;

import static org.junit.Assert.fail;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common.TimePeriods;

/**
 * Test that the truncation works according to the design.
 * 
 * @author Terry Packer
 */
public class TruncateTimePeriodAdjusterTest {

    protected final int START_YEAR = 2015;
    protected final int FINAL_YEAR = 2020; //29;
    
    interface TruncationTest {
        void test(ZonedDateTime time);
    }
    interface TimeIncrementor {
        ZonedDateTime incrementTime(ZonedDateTime time); //Increment for next test
    }
    
    @Test
    public void testMilliseconds() {
        for(int i=0; i<=1000; i++) {
            final int j = i == 0 ? 1 : i; //avoid div 0 with %0
            TruncateTimePeriodAdjuster adjuster = new TruncateTimePeriodAdjuster(TimePeriods.MILLISECONDS, i);
            test((time) ->{
                ZonedDateTime result = (ZonedDateTime)adjuster.adjustInto(time);
                int milli = result.get(ChronoField.MILLI_OF_SECOND);
                if(milli != time.get(ChronoField.MILLI_OF_SECOND) - (time.get(ChronoField.MILLI_OF_SECOND) % j))
                    fail("Expected " + (time.get(ChronoField.MILLI_OF_SECOND) - (time.get(ChronoField.MILLI_OF_SECOND) % j)) + 
                            " for " + j + " at time " + time.get(ChronoField.MILLI_OF_SECOND) +
                            " got millisecond field " + milli);
            }, (time) -> {
                time = incrementDateTime(time, TimePeriods.MILLISECONDS, 999);
                return incrementDateTime(time, TimePeriods.DAYS, 1);
            });
        }
    }
    
    @Test
    public void testSeconds() {
        for(int i=0; i<=1000; i++) {
            final int j = i == 0 ? 1 : i; //avoid div 0 with %0
            TruncateTimePeriodAdjuster adjuster = new TruncateTimePeriodAdjuster(TimePeriods.SECONDS, i);
            test((time) ->{
                ZonedDateTime result = (ZonedDateTime)adjuster.adjustInto(time);
                int second = result.get(ChronoField.SECOND_OF_MINUTE);
                if(second != time.get(ChronoField.SECOND_OF_MINUTE) - (time.get(ChronoField.SECOND_OF_MINUTE) % j))
                    fail("Expected " + (time.get(ChronoField.SECOND_OF_MINUTE) - (time.get(ChronoField.SECOND_OF_MINUTE) % j)) + 
                            " for " + j + " at time " + time.get(ChronoField.SECOND_OF_MINUTE) +
                            " got second field " + second);
            }, (time) -> {
                time = incrementDateTime(time, TimePeriods.SECONDS, 59);
                return incrementDateTime(time, TimePeriods.DAYS, 1);
            });
        }
    }
    
    @Test
    public void testMinutes() {
        for(int i=0; i<=1000; i++) {
            final int j = i == 0 ? 1 : i; //avoid div 0 with %0
            TruncateTimePeriodAdjuster adjuster = new TruncateTimePeriodAdjuster(TimePeriods.MINUTES, i);
            test((time) ->{
                ZonedDateTime result = (ZonedDateTime)adjuster.adjustInto(time);
                int minute = result.get(ChronoField.MINUTE_OF_HOUR);
                if(minute != time.get(ChronoField.MINUTE_OF_HOUR) - (time.get(ChronoField.MINUTE_OF_HOUR) % j))
                    fail("Expected " + (time.get(ChronoField.MINUTE_OF_HOUR) - (time.get(ChronoField.MINUTE_OF_HOUR) % j)) + 
                            " for " + j + " at time " + time.get(ChronoField.MINUTE_OF_HOUR) +
                            " got minute field " + minute);
            }, (time) -> {
                time = incrementDateTime(time, TimePeriods.MINUTES, 59);
                return incrementDateTime(time, TimePeriods.DAYS, 1);
            });
        }
    }
    
    @Test
    public void testHours() {
        for(int i=5; i<=1000; i++) {
            final int j = i == 0 ? 1 : i; //avoid div 0 with %0
            TruncateTimePeriodAdjuster adjuster = new TruncateTimePeriodAdjuster(TimePeriods.HOURS, i);
            test((time) ->{
                ZonedDateTime result = (ZonedDateTime)adjuster.adjustInto(time);
                int hour = result.get(ChronoField.HOUR_OF_DAY);
                int expected = time.get(ChronoField.HOUR_OF_DAY) - (time.get(ChronoField.HOUR_OF_DAY) % j);
                if(hour != expected)
                    fail("Expected " + expected + 
                            " for " + j + " at time " + time.get(ChronoField.HOUR_OF_DAY) +
                            " got hour field " + hour + " for date " + time.toString());
            }, (time) -> {
                return incrementDateTime(time, TimePeriods.HOURS, 23);
            });
        }
    }
    
    @Test
    public void testDays() {
        for(int i=0; i<=365; i++) {
            final int j = i == 0 ? 1 : i; //avoid div 0 with %0
            TruncateTimePeriodAdjuster adjuster = new TruncateTimePeriodAdjuster(TimePeriods.DAYS, i);
            test((time) ->{
                ZonedDateTime result = (ZonedDateTime)adjuster.adjustInto(time);
                int doy = result.get(ChronoField.DAY_OF_YEAR);
                if(doy != time.get(ChronoField.DAY_OF_YEAR) - ((time.get(ChronoField.DAY_OF_YEAR)-1) % j))
                    fail("Expected " + (time.get(ChronoField.DAY_OF_YEAR) - ((time.get(ChronoField.DAY_OF_YEAR)-1) % j)) + 
                            " for " + j + " at time " + time.get(ChronoField.DAY_OF_YEAR) +
                            " got day of year field " + doy);
            }, (time) -> {
                return incrementDateTime(time, TimePeriods.DAYS, 1);
            });
        }
    }
    
    @Test
    public void testWeeks() {
        for(int i=0; i<=100; i++) {
            final int j = i == 0 ? 1 : i; //avoid div 0 with %0
            TruncateTimePeriodAdjuster adjuster = new TruncateTimePeriodAdjuster(TimePeriods.WEEKS, i);
            test((time) ->{
                ZonedDateTime result = (ZonedDateTime)adjuster.adjustInto(time);
                int woy = result.get(ChronoField.ALIGNED_WEEK_OF_YEAR);
                int doy = result.get(ChronoField.DAY_OF_YEAR);
                if(woy != time.get(ChronoField.ALIGNED_WEEK_OF_YEAR) - ((time.get(ChronoField.ALIGNED_WEEK_OF_YEAR)-1) % j))
                    fail("Expected " + (time.get(ChronoField.ALIGNED_WEEK_OF_YEAR) - ((time.get(ChronoField.ALIGNED_WEEK_OF_YEAR)-1) % j)) + 
                            " for " + j + " at time " + time.get(ChronoField.ALIGNED_WEEK_OF_YEAR) +
                            " got week of year field " + woy);
                if((doy-1) % 7 != 0)
                    fail("Starting with the wrong aligned day of the week.");
            }, (time) -> {
                return incrementDateTime(time, TimePeriods.DAYS, 8);
            });
        }
    }
    
    @Test
    public void testMonths() {
        for(int i=0; i<=100; i++) {
            final int j = i == 0 ? 1 : i; //avoid div 0 with %0
            TruncateTimePeriodAdjuster adjuster = new TruncateTimePeriodAdjuster(TimePeriods.MONTHS, i);
            test((time) ->{
                ZonedDateTime result = (ZonedDateTime)adjuster.adjustInto(time);
                int month = result.get(ChronoField.MONTH_OF_YEAR);
                if(month != time.get(ChronoField.MONTH_OF_YEAR) - ((time.get(ChronoField.MONTH_OF_YEAR)-1) % j))
                    fail("Expected " + (time.get(ChronoField.MONTH_OF_YEAR) - ((time.get(ChronoField.MONTH_OF_YEAR)-1) % j)) + 
                            " for " + j + " at time " + time.get(ChronoField.MONTH_OF_YEAR) +
                            " got month field " + month);
            }, (time) -> {
                return incrementDateTime(time, TimePeriods.DAYS, 8);
            });
        }
    }
    
    @Test
    public void testYears() {
        for(int i=0; i<=20; i++) {
            final int j = i == 0 ? 1 : i; //avoid div 0 with %0
            TruncateTimePeriodAdjuster adjuster = new TruncateTimePeriodAdjuster(TimePeriods.YEARS, i);
            test((time) ->{
                ZonedDateTime result = (ZonedDateTime)adjuster.adjustInto(time);
                int year = result.get(ChronoField.YEAR);
                if(year != time.get(ChronoField.YEAR) - ((time.get(ChronoField.YEAR)%100) % j)) //Year starts at 0
                    fail("Expected " + (time.get(ChronoField.YEAR) - (time.get(ChronoField.YEAR) % j)) + 
                            " for " + j + " at time " + time.get(ChronoField.YEAR) +
                            " got year field " + year);
            }, (time) -> {
                return incrementDateTime(time, TimePeriods.DAYS, 166);
            });
        }
    }
    
    //Test for each Seconds -> Years
    
    /**
     * Run the test over a given range of dates with a TimePeriodType increment
     * @param type - time period type to increment after each test
     * @param t - test to run
     */
    protected void test(TruncationTest t, TimeIncrementor i) {
        
        Instant start = LocalDateTime.of(START_YEAR, 1, 1, 0, 0).atZone(ZoneId.systemDefault()).toInstant();
        Instant end = LocalDateTime.of(FINAL_YEAR, 1, 1, 0, 0).atZone(ZoneId.systemDefault()).toInstant();
        
        while(start.isBefore(end)) {
            ZonedDateTime zdt = start.atZone(ZoneId.systemDefault());
            t.test(zdt);
            start = i.incrementTime(zdt).toInstant();
        }
    }
    
    protected ZonedDateTime incrementDateTime(ZonedDateTime start, int type, int periods) {
        switch(type) {
            case TimePeriods.DAYS:
                return start.plus(periods, ChronoUnit.DAYS);
            case TimePeriods.HOURS:
                return start.plus(periods, ChronoUnit.HOURS);
            case TimePeriods.MILLISECONDS:
                return start.plus(periods, ChronoUnit.MILLIS);
            case TimePeriods.MINUTES:
                return start.plus(periods, ChronoUnit.MINUTES);
            case TimePeriods.MONTHS:
                return start.plus(periods, ChronoUnit.MONTHS);
            case TimePeriods.SECONDS:
                return start.plus(periods, ChronoUnit.SECONDS);
            case TimePeriods.WEEKS:
                return start.plus(periods, ChronoUnit.WEEKS);
            case TimePeriods.YEARS:
                return start = start.plus(periods, ChronoUnit.YEARS);
            default:
                throw new ShouldNeverHappenException("Invalid period type");
        }
    }
}
