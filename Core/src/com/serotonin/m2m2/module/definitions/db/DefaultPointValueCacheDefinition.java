/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.module.definitions.db;

import org.springframework.beans.factory.annotation.Autowired;

import com.infiniteautomation.mango.pointvalue.DefaultPointValueCacheDao;
import com.infiniteautomation.mango.pointvalue.PointValueCacheDao;
import com.serotonin.m2m2.db.PointValueCacheDefinition;
import com.serotonin.m2m2.db.dao.PointValueDao;

/**
 * Default implementation to use when no other {@link PointValueCacheDefinition} is enabled.
 */
public class DefaultPointValueCacheDefinition extends PointValueCacheDefinition {

    @Autowired
    private PointValueDao pointValueDao;
    private DefaultPointValueCacheDao pointValueCache;

    @Override
    protected void initialize() {
        this.pointValueCache = new DefaultPointValueCacheDao(pointValueDao);
    }

    @Override
    public void shutdown() {
        // nothing to do
    }

    @Override
    public PointValueCacheDao getPointValueCache() {
        return pointValueCache;
    }

    @Override
    public int getOrder() {
        //Lowest priority
        return Integer.MAX_VALUE;
    }
}
