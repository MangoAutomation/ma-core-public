/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 * @Author Terry Packer
 *
 */

package com.serotonin.m2m2.module.definitions.db;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.PointValueCacheProxy;
import com.serotonin.m2m2.db.dao.PointValueCacheDao;

/**
 * Default implementation of proxy to use if none are module defined
 */
public class DefaultPointValueCacheProxy extends PointValueCacheProxy {

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
