/**
 * @copyright 2018 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.util.datetime;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import com.infiniteautomation.mango.util.datetime.ExpandTimePeriodAdjuster;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common.TimePeriods;
import com.serotonin.m2m2.web.mvc.rest.v1.model.time.TimePeriodType;

/**
 *
 * @author Terry Packer
 */
public class ExpandTimePeriodAdjusterTest {
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
        ZonedDateTime start = LocalDateTime.of(START_YEAR, 1, 1, 0, 0, 0, 0).atZone(ZoneId.systemDefault());
        ExpandTimePeriodAdjuster adjuster = new ExpandTimePeriodAdjuster(start, TimePeriods.MILLISECONDS, 100);
        
        ZonedDateTime now = LocalDateTime.of(START_YEAR, 1, 1, 0, 0, 0, 1).atZone(ZoneId.systemDefault());
        now = now.with(adjuster);
        assertEquals(100, now.get(ChronoField.MILLI_OF_SECOND));
    }
    
    @Test
    public void testSeconds() {
        ZonedDateTime start = LocalDateTime.of(START_YEAR, 1, 1, 0, 0, 0, 0).atZone(ZoneId.systemDefault());
        ExpandTimePeriodAdjuster adjuster = new ExpandTimePeriodAdjuster(start, TimePeriods.SECONDS, 10);
        
        ZonedDateTime now = LocalDateTime.of(START_YEAR, 1, 1, 0, 0, 0, 1).atZone(ZoneId.systemDefault());
        now = now.with(adjuster);
        assertEquals(10, now.get(ChronoField.SECOND_OF_MINUTE));
    }
    
    @Test
    public void testMinutes() {
        ZonedDateTime start = LocalDateTime.of(START_YEAR, 1, 1, 0, 0, 0, 0).atZone(ZoneId.systemDefault());
        ExpandTimePeriodAdjuster adjuster = new ExpandTimePeriodAdjuster(start, TimePeriods.MINUTES, 10);
        
        ZonedDateTime now = LocalDateTime.of(START_YEAR, 1, 1, 0, 0, 0, 1).atZone(ZoneId.systemDefault());
        now = now.with(adjuster);
        assertEquals(10, now.get(ChronoField.MINUTE_OF_HOUR));
    }
    
    @Test
    public void testHours() {
        ZonedDateTime start = LocalDateTime.of(START_YEAR, 1, 1, 0, 0, 0, 0).atZone(ZoneId.systemDefault());
        ExpandTimePeriodAdjuster adjuster = new ExpandTimePeriodAdjuster(start, TimePeriods.HOURS, 10);
        
        ZonedDateTime now = LocalDateTime.of(START_YEAR, 1, 1, 10, 0, 0, 1).atZone(ZoneId.systemDefault());
        now = now.with(adjuster);
        assertEquals(20, now.get(ChronoField.HOUR_OF_DAY));
    }
    
    @Test
    public void testDays() {
        ZonedDateTime start = LocalDateTime.of(START_YEAR, 1, 1, 0, 0, 0, 0).atZone(ZoneId.systemDefault());
        ExpandTimePeriodAdjuster adjuster = new ExpandTimePeriodAdjuster(start, TimePeriods.DAYS, 10);
        
        ZonedDateTime now = LocalDateTime.of(START_YEAR, 1, 11, 0, 0, 0, 1).atZone(ZoneId.systemDefault());
        now = now.with(adjuster);
        assertEquals(21, now.get(ChronoField.DAY_OF_MONTH));
    }
    
    @Test
    public void testMonths() {
        ZonedDateTime start = LocalDateTime.of(START_YEAR, 1, 1, 0, 0, 0, 0).atZone(ZoneId.systemDefault());
        ExpandTimePeriodAdjuster adjuster = new ExpandTimePeriodAdjuster(start, TimePeriods.MONTHS, 6);
        
        ZonedDateTime now = LocalDateTime.of(START_YEAR, 7, 1, 0, 0, 0, 1).atZone(ZoneId.systemDefault());
        now = now.with(adjuster);
        assertEquals(1, now.get(ChronoField.MONTH_OF_YEAR));
    }
    
    @Test
    public void testYears() {
        ZonedDateTime start = LocalDateTime.of(START_YEAR, 1, 1, 0, 0, 0, 0).atZone(ZoneId.systemDefault());
        ExpandTimePeriodAdjuster adjuster = new ExpandTimePeriodAdjuster(start, TimePeriods.YEARS, 10);
        
        ZonedDateTime now = LocalDateTime.of(START_YEAR, 1, 1, 0, 0, 0, 1).atZone(ZoneId.systemDefault());
        now = now.with(adjuster);
        assertEquals(START_YEAR + 10, now.get(ChronoField.YEAR));
    }
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
    
    protected ZonedDateTime incrementDateTime(ZonedDateTime start, TimePeriodType type, int periods) {
        switch(type) {
            case DAYS:
                return start.plus(periods, ChronoUnit.DAYS);
            case HOURS:
                return start.plus(periods, ChronoUnit.HOURS);
            case MILLISECONDS:
                return start.plus(periods, ChronoUnit.MILLIS);
            case MINUTES:
                return start.plus(periods, ChronoUnit.MINUTES);
            case MONTHS:
                return start.plus(periods, ChronoUnit.MONTHS);
            case SECONDS:
                return start.plus(periods, ChronoUnit.SECONDS);
            case WEEKS:
                return start.plus(periods, ChronoUnit.WEEKS);
            case YEARS:
                return start = start.plus(periods, ChronoUnit.YEARS);
            default:
                throw new ShouldNeverHappenException("Invalid period type");
        }
    }
}
