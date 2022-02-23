/*
 * Copyright (C) 2022 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao.pointvalue;

import com.serotonin.m2m2.rt.dataImage.types.DataValue;

/**
 * @author Jared Wiltshire
 */
public interface AggregateValue {

    long getPeriodStartTime();

    long getPeriodEndTime();

    DataValue getStartValue();

    DataValue getFirstValue();

    Long getFirstTime();

    DataValue getLastValue();

    Long getLastTime();

    long getCount();
}
