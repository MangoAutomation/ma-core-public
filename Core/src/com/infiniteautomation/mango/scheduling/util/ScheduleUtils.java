/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.scheduling.util;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.serotonin.ShouldNeverHappenException;

/**
 * 
 * Utils for working with Weekly/Daily Schedules
 *
 * @author Terry Packer
 */
public class ScheduleUtils {
    
    public static final long SECONDS_IN_DAY = 86400;

    public static TimeValue parseTimeValue(String time) {
        TimeValue value = new TimeValue();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("[HH:mm:ss.SSS][HH:mm:ss][HH:mm]");
        LocalTime parsed = LocalTime.parse(time, formatter);
        value.setHour(parsed.getHour());
        value.setMinute(parsed.getMinute());
        value.setSecond(parsed.getSecond());
        value.setMillisecond(parsed.getNano()/1000000);
        return value;
    }

    /**
     * Create a Zoned Date Time from an epoch
     *
     */
    public static ZonedDateTime createZonedDateTime(long runtime) {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(runtime), getTimeZone());
    }

    public static ZoneId getTimeZone() {
        return ZoneId.systemDefault();
    }
    
    /**
     * Create a ZonedDateTime at start of day on Sunday being the previous Sunday to today unless
     *   today is Sunday then we return start of today.
     */
    public static ZonedDateTime getStartOfLastSunday(ZonedDateTime time) {
        //TODO https://stackoverflow.com/questions/29143910/java-8-date-time-get-start-of-day-from-zoneddatetime
        //TODO https://stackoverflow.com/questions/28450720/get-date-of-first-day-of-week-based-on-localdate-now-in-java-8
        //TODO This code won't work properly in Locales that have Monday as start of week.
        //return time.with(DayOfWeek.SUNDAY).toLocalDate().atStartOfDay(time.getZone());
        //TODO Try this: 
        if(time.getDayOfWeek() == DayOfWeek.SUNDAY)
            return time.toLocalDate().atStartOfDay(time.getZone());
        else
            return time.toLocalDate().with(java.time.temporal.TemporalAdjusters.previous(DayOfWeek.SUNDAY)).atStartOfDay(time.getZone());
    }

    /**
     * Create a ZonedDateTime at the start of the day the date is in
     */
    public static ZonedDateTime getStartOfDay(ZonedDateTime time) {
        //TODO https://stackoverflow.com/questions/29143910/java-8-date-time-get-start-of-day-from-zoneddatetime
        return time.toLocalDate().atStartOfDay(time.getZone());
    }
    
    /**
     * Check if 'toCheck' is on or after the date portion of 'date'
     */
    public static boolean isDateOnOrAfter(ZonedDateTime date, ZonedDateTime toCheck) {
        LocalDate localDate = date.toLocalDate();
        LocalDate localToCheck = toCheck.toLocalDate();
        if(localDate.equals(localToCheck))
            return true;
        else if(localToCheck.isAfter(localDate))
            return true;
        else
            return false;
    }
    
    /**
     * Check if 'toCheck' is on or before the date portion of 'date'
     */
    public static boolean isDateOnOrBefore(ZonedDateTime date, ZonedDateTime toCheck) {
        LocalDate localDate = date.toLocalDate();
        LocalDate localToCheck = toCheck.toLocalDate();
        if(localDate.equals(localToCheck))
            return true;
        else if(localToCheck.isBefore(localDate))
            return true;
        else
            return false;
    }
    
    
    /**
     * Get the array index for a day of week 1=Sunday - 7=Saturday
     */
    public static int getDayOfWeekIndex(DayOfWeek dow) {
        switch(dow) {
            case SUNDAY:
                return 1;
            case MONDAY:
                return 2;
            case TUESDAY:
                return 3;
            case WEDNESDAY:
                return 4;
            case THURSDAY:
                return 5;
            case FRIDAY:
                return 6;
            case SATURDAY:
                return 7;
            default:
                throw new ShouldNeverHappenException("Not a day of week.");
        }
    }
    
    public static DayOfWeek getDayOfWeekFromIndex(int index) {
        switch(index) {
            case 1:
                return DayOfWeek.SUNDAY;
            case 2:
                return DayOfWeek.MONDAY;
            case 3:
                return DayOfWeek.TUESDAY;
            case 4:
                return DayOfWeek.WEDNESDAY;
            case 5:
                return DayOfWeek.THURSDAY;
            case 6:
                return DayOfWeek.FRIDAY;
            case 7:
                return DayOfWeek.SATURDAY;
            default:
                throw new ShouldNeverHappenException("Not a day of week.");
        }
    }

    /**
     * Is a before b in our Days of Week listing
     *
     */
    public static boolean isBefore(DayOfWeek a, DayOfWeek b) {
        int indexA = getDayOfWeekIndex(a);
        int indexB = getDayOfWeekIndex(b);
        if(indexA - indexB < 0)
            return true;
        else
            return false;
    }

    /**
     * Is a after b in our Days of Week listing
     */
    public static boolean isAfter(DayOfWeek a, DayOfWeek b) {
        int indexA = getDayOfWeekIndex(a);
        int indexB = getDayOfWeekIndex(b);
        if(indexA - indexB > 0)
            return true;
        else
            return false;
    }
    
}
