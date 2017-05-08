/*
    Copyright (C) 2006-2007 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.web.taglib;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.serotonin.util.StringUtils;

/**
 * @author Matthew Lohbihler
 */
public class DateFunctions {
    public static DateTimeFormatter dtfFullMinute = DateTimeFormat.forPattern("yyyy/MM/dd HH:mm");
    public static DateTimeFormatter dtfFullSecond = DateTimeFormat.forPattern("yyyy/MM/dd HH:mm:ss");
    public static DateTimeFormatter dtfLong = DateTimeFormat.forPattern("yyyy/MM/dd");
    public static DateTimeFormatter dtfMed = DateTimeFormat.forPattern("MMM dd HH:mm");
    public static DateTimeFormatter dtfShort = DateTimeFormat.forPattern("HH:mm:ss");
    
    public static String getTime(long time) {
        DateTime valueTime = new DateTime(time);
        DateTime now = new DateTime();
        
        if (valueTime.getYear() != now.getYear())
            return dtfLong.print(valueTime);
        
        if (valueTime.getMonthOfYear() != now.getMonthOfYear() || valueTime.getDayOfMonth() != now.getDayOfMonth())
            return dtfMed.print(valueTime);
        
        return dtfShort.print(valueTime);
    }
    
    public static String getFullSecondTime(long time) {
        return dtfFullSecond.print(new DateTime(time));
    }
    
    public static String getFullMinuteTime(long time) {
        return dtfFullMinute.print(new DateTime(time));
    }
    
    public static String getDuration(long duration) {
        return StringUtils.durationToString(duration);
    }
    
    public static void main(String[] args) {
        System.out.println(getFullSecondTime(1200884728319l));
    }
}
 