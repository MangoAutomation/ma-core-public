/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.module.definitions.db;

import com.infiniteautomation.mango.pointvalue.PointValueCacheDao;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.PointValueCacheDefinition;

/**
 * Default implementation to use when no other {@link PointValueCacheDefinition} is enabled.
 */
public class DefaultPointValueCacheDefinition extends PointValueCacheDefinition {

    @Override
    protected void initialize() {
    }

    @Override
    public void shutdown() {

    }

    @Override
    public PointValueCacheDao getDao() {
        return (PointValueCacheDao) Common.databaseProxy.newPointValueDao();
    }

    @Override
    public int getOrder() {
        //Lowest priority
        return Integer.MAX_VALUE;
    }
}
