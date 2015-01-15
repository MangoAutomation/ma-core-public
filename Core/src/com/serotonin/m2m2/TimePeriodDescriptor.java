/*
 * Copyright (C) 2013 Deltamation Software. All rights reserved.
 * @author Jared Wiltshire
 */

package com.serotonin.m2m2;

import java.util.HashMap;
import java.util.Map;

/**
 * Copyright (C) 2013 Deltamation Software. All rights reserved.
 * @author Jared Wiltshire
 */
public enum TimePeriodDescriptor {
    
    // Past periods relative to the current time
    
    /**
     * The past 24 hours, e.g. 13:00 yesterday until now (13:00)
     */
    PREVIOUS_DAY(1),
    /**
     * The past 7 days, e.g. Wed 13:00 last week until now (Wed 13:00)
     */
    PREVIOUS_WEEK(7),
    /**
     * The past 4 weeks, e.g. Wed 4 weeks ago until now (this Wed)
     */
    PREVIOUS_4WEEKS(28),
    /**
     * The past month, e.g. from the 12th Feb until now (12th Mar)
     */
    PREVIOUS_MONTH(5),
    /**
     * The past year, e.g. from 12th Feb last year until now (12 Feb this year)
     */
    PREVIOUS_YEAR(6),

    // Last whole periods
    
    /**
     * The whole of yesterday, e.g. 00:00 to 23:59 yesterday
     */
    YESTERDAY(4),
    /**
     * The whole of last week, e.g. Sunday 00:00 to Sunday 23:59
     */
    LAST_WEEK(3),
    /**
     * The whole of last month, e.g. 1st Feb to 28th Feb
     */
    LAST_MONTH(2),
    /**
     * The whole of last year, e.g. from 1st Jan to 31st Dec
     */
    LAST_YEAR(8),
    
    // To date periods
    
    /**
     * From the start of the day until now, e.g. from 00:00 until now
     */
    DAY_TO_DATE(9),
    /**
     * From the start of the week until now, e.g. from 00:00 Sunday until now
     */
    WEEK_TO_DATE(10),
    /**
     * From the start of the month until now, e.g. from 00:00 on the 1st until now
     */
    MONTH_TO_DATE(11),
    /**
     * From the start of the year until now, e.g. from the 1st Jan until now
     */
    YEAR_TO_DATE(12),
    
    // Fixed time periods, periods up till current time etc
    
    /**
     * A fixed time period with the start date and end date given
     */
    FIXED_TO_FIXED (0),
    /**
     * From a given start date up until the current time
     */
    FIXED_TO_NOW(-1),
    /**
     * From the first value on record up until a given end date
     */
    INCEPTION_TO_FIXED(-2),
    /**
     * From the first value on record up until the current time
     */
    INCEPTION_TO_NOW(-3);
    
    private int code;
    
    private TimePeriodDescriptor(int code) {
        this.code = code;
    }
    
    public int getCode() {
        return code;
    }
    
    private static final Map<Integer, TimePeriodDescriptor> map = new HashMap<Integer, TimePeriodDescriptor>();
    static
    {
        for (TimePeriodDescriptor period : TimePeriodDescriptor.values())
            map.put(period.code, period);
    }
    
    public static TimePeriodDescriptor from(int value) {
        return map.get(value);
    }
}
