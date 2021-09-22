/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.infiniteautomation.mango.pointvalue.PointValueCacheDao;
import com.serotonin.m2m2.module.ModuleElementDefinition;

/**
 * Definition that provides a {@link PointValueCacheDao} for accessing point value caches.
 */
public abstract class PointValueCacheDefinition extends ModuleElementDefinition {

    /**
     * Initialize the {@link PointValueCacheDao}
     */
    @PostConstruct
    public abstract void initialize();

    /**
     * Terminate the {@link PointValueCacheDao}
     */
    @PreDestroy
    public abstract void shutdown();

    /**
     * @return a singleton, thread safe instance of the {@link PointValueCacheDao} implementation
     */
    public abstract PointValueCacheDao getPointValueCache();

}
