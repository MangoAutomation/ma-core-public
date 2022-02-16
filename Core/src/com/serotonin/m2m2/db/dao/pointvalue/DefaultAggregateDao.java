/*
 * Copyright (C) 2022 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao.pointvalue;

import java.time.temporal.TemporalAmount;

import com.serotonin.m2m2.db.dao.PointValueDao;

/**
 * @author Jared Wiltshire
 */
public class DefaultAggregateDao implements AggregateDao {

    private final PointValueDao pointValueDao;
    private final TemporalAmount aggregationPeriod;

    public DefaultAggregateDao(PointValueDao pointValueDao, TemporalAmount aggregationPeriod) {
        this.pointValueDao = pointValueDao;
        this.aggregationPeriod = aggregationPeriod;
    }

    @Override
    public PointValueDao getPointValueDao() {
        return pointValueDao;
    }

    @Override
    public TemporalAmount getAggregationPeriod() {
        return aggregationPeriod;
    }
}
