/*
 * Copyright (C) 2022 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao.pointvalue;

import com.serotonin.m2m2.db.dao.PointValueDao;

/**
 * @author Jared Wiltshire
 */
public class DefaultAggregateDao implements AggregateDao {

    private final PointValueDao pointValueDao;

    public DefaultAggregateDao(PointValueDao pointValueDao) {
        this.pointValueDao = pointValueDao;
    }

    @Override
    public PointValueDao getPointValueDao() {
        return pointValueDao;
    }

}
