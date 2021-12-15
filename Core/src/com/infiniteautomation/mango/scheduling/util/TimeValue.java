/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.scheduling.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.zone.ZoneOffsetTransition;

/**
 * Container for an offset of time within a daily schedule
 *
 * @author Terry Packer
 */
public class TimeValue implements Comparable<TimeValue>{
    
    private int hour;
    private int minute;
    private int second;
    private int millisecond;
    
    /**
     * @return the hour
     */
    public int getHour() {
        return hour;
    }
    /**
     * @param hour the hour to set
     */
    public void setHour(int hour) {
        this.hour = hour;
    }
    /**
     * @return the minute
     */
    public int getMinute() {
        return minute;
    }
    /**
     * @param minute the minute to set
     */
    public void setMinute(int minute) {
        this.minute = minute;
    }
    /**
     * @return the second
     */
    public int getSecond() {
        return second;
    }
    /**
     * @param second the second to set
     */
    public void setSecond(int second) {
        this.second = second;
    }
    /**
     * @return the millisecond
     */
    public int getMillisecond() {
        return millisecond;
    }
    /**
     * @param millisecond the millisecond to set
     */
    public void setMillisecond(int millisecond) {
        this.millisecond = millisecond;
    }
    
    /**
     * Get the nanosecond part of the offset
     */
    public int getNano() {
        return this.millisecond * 1000000;
    }
    
    
    /**
     */
    public static TimeValue get(ZonedDateTime runtime) {
        TimeValue value = new TimeValue();
        value.setHour(runtime.getHour());
        value.setMinute(runtime.getMinute());
        value.setSecond(runtime.getSecond());
        value.setMillisecond(runtime.getNano()/1000000);
        return value;
  }
    
    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(TimeValue that) {
        if(hour == that.hour) {
            if(minute == that.minute) {
                if(second == that.second) {
                    return millisecond - that.millisecond;
                }else {
                    return second - that.second;
                }
            }else {
                return minute - that.minute;
            }
        }
        return hour - that.hour;
    }
    
    /**
     * Get the nanoseconds since midnight of a day
     */
    public long getNanos() {
       long nanos = ((long)hour)*3600000000000l;
       nanos += ((long)minute)*60000000000l;
       nanos += ((long)second)*1000000000l;
       nanos += ((long)millisecond*1000000l);
       return nanos;
    }
    
    /**
     * get the seconds since midnight of a day
     */
    public long getSeconds() {
        long seconds = ((long)hour)*3600l;
        seconds += ((long)minute)*60l;
        seconds += ((long)second);
        return seconds;
    }
    
    @Override
    public String toString() {
        return String.format("%02d:%02d:%02d.%03d", hour, minute, second, millisecond);
    }
    /**
     */
    public boolean isMidnight() {
        return hour==0&&minute==0&&second==0&&millisecond==0;
    }
    /**
     */
    public Instant getInstant(ZonedDateTime zdt) {
        ZonedDateTime offset = zdt.withHour(hour).withMinute(minute).withSecond(second).withNano((int)(millisecond * 1000000l));
        LocalDateTime ldt = zdt.toLocalDateTime();
        ldt = ldt.withHour(hour).withMinute(minute).withSecond(second).withNano((int)(millisecond * 1000000l));
        
        if(!zdt.getZone().getRules().isValidOffset(ldt, zdt.getOffset())) {
            //Within a gap of DST so OR after the transition
            ZoneOffsetTransition transition = zdt.getZone().getRules().nextTransition(zdt.toInstant());
            if(!ldt.isAfter(transition.getDateTimeAfter())){
                //In a gap so we shift our time forward to the end of the gap.
                offset = transition.getDateTimeAfter().atZone(zdt.getZone());
            }else {
                //After a gap so ensure we use the next zone offset
                offset = ldt.atOffset(transition.getOffsetAfter()).atZoneSimilarLocal(transition.getOffsetAfter());
            }
        }
        
        return offset.toInstant();
    }
}
