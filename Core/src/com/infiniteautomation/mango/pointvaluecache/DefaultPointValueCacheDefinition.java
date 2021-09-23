/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.pointvaluecache;

import org.springframework.beans.factory.annotation.Autowired;

import com.serotonin.m2m2.db.dao.PointValueDao;

/**
 * Default implementation to use when no other {@link PointValueCacheDefinition} is enabled.
 */
public class DefaultPointValueCacheDefinition extends PointValueCacheDefinition {

    @Autowired
    private PointValueDao pointValueDao;
    private DefaultPointValueCache pointValueCache;

    @Override
    public void initialize() {
        this.pointValueCache = new DefaultPointValueCache(pointValueDao);
    }

    @Override
    public void shutdown() {
        // nothing to do
    }

    @Override
    public PointValueCache getPointValueCache() {
        return pointValueCache;
    }

    @Override
    public int getOrder() {
        //Lowest priority
        return Integer.MAX_VALUE;
    }
}
