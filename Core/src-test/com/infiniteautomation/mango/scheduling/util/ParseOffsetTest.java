/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.scheduling.util;

import org.junit.Assert;
import org.junit.Test;



/**
 * Test the valid input formats for time offests
 *
 * @author Terry Packer
 */
public class ParseOffsetTest {

    @Test
    public void parseFull() {
        TimeValue tv = ScheduleUtils.parseTimeValue("08:01:02.003"); 
        Assert.assertEquals(8, tv.getHour());
        Assert.assertEquals(1, tv.getMinute());
        Assert.assertEquals(2, tv.getSecond());
        Assert.assertEquals(3, tv.getMillisecond());
    }
    
    @Test
    public void parseHHmmss() {
        TimeValue tv = ScheduleUtils.parseTimeValue("08:01:02"); 
        Assert.assertEquals(8, tv.getHour());
        Assert.assertEquals(1, tv.getMinute());
        Assert.assertEquals(2, tv.getSecond());
        Assert.assertEquals(0, tv.getMillisecond());
    }
    
    @Test
    public void parseHHmm() {
        TimeValue tv = ScheduleUtils.parseTimeValue("08:01"); 
        Assert.assertEquals(8, tv.getHour());
        Assert.assertEquals(1, tv.getMinute());
        Assert.assertEquals(0, tv.getSecond());
        Assert.assertEquals(0, tv.getMillisecond());
    }
}
