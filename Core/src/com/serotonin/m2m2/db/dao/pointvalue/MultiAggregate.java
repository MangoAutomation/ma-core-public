/*
 * Copyright (C) 2022 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao.pointvalue;

/**
 * @author Jared Wiltshire
 */
public interface MultiAggregate extends AggregateValue {

    default boolean isInPeriod(AggregateValue child) {
        long startTime = child.getPeriodStartTime();
        return startTime >= getPeriodStartTime() && startTime < getPeriodEndTime();
    }

    void addChild(AggregateValue child);
}
