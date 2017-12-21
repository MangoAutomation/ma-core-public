/**
 * @copyright 2017 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.util.datetime;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common.TimePeriods;
import com.serotonin.m2m2.web.mvc.rest.v1.model.time.TimePeriodType;

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
            TruncateTimePeriodAdjuster adjuster = new TruncateTimePeriodAdjuster(TimePeriods.MILLISECONDS, i);
            test((time) ->{
                ZonedDateTime result = (ZonedDateTime)adjuster.adjustInto(time);
                //Ensure result is truncated to the ith milli
                //System.out.println(result.toInstant().toEpochMilli());    
                //TODO Perform assertion here
            }, (time) -> {
                time = incrementDateTime(time, TimePeriodType.MILLISECONDS, 999);
                return incrementDateTime(time, TimePeriodType.DAYS, 1);
            });
        }
    }
    
    @Test
    public void testDays() {
        for(int i=0; i<=365; i++) {
            TruncateTimePeriodAdjuster adjuster = new TruncateTimePeriodAdjuster(TimePeriods.DAYS, i);
            test((time) ->{
                ZonedDateTime result = (ZonedDateTime)adjuster.adjustInto(time);
                //Ensure result is truncated to the ith milli
                //System.out.println(result.toInstant().toEpochMilli());    
                //TODO Perform assertion here
            }, (time) -> {
                return incrementDateTime(time, TimePeriodType.DAYS, 1);
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
