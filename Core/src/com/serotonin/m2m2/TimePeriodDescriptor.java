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
    PREVIOUS_DAY(1),
    PREVIOUS_WEEK(7),
    PREVIOUS_4WEEKS(28),
    FIXED_TO_FIXED (0),
    FIXED_TO_NOW(-1),
    INCEPTION_TO_FIXED(-2),
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
