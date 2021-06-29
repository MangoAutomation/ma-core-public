/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.quantize;

import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 *
 * @author Terry Packer
 */
public abstract class BaseQuantizerTest {

    protected final int finalNano = 999999999;
    protected ZoneId zoneId;
    protected ZonedDateTime from;
    protected ZonedDateTime to;
    
    //Useful for testing in callbacks
    protected ZonedDateTime time;
    
    public BaseQuantizerTest() {
        this.zoneId = ZoneId.of("UTC-10");
        this.from = ZonedDateTime.of(2017, 01, 01, 00, 00, 00, 0, zoneId);
        this.to = ZonedDateTime.of(2017, 02, 01, 00, 00, 00, 0, zoneId);
    }
    
}
