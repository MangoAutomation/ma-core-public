/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.pointvaluecache;

import javax.annotation.PreDestroy;

import com.serotonin.m2m2.module.ModuleElementDefinition;

/**
 * Definition that provides a {@link PointValueCache} for accessing point value caches.
 */
public abstract class PointValueCacheDefinition extends ModuleElementDefinition {

    /**
     * Initialize the {@link PointValueCache}
     * Note: Only the highest priority definition should be initialized by {@link com.infiniteautomation.mango.spring.MangoRuntimeContextConfiguration#latestPointValueDao MangoRuntimeContextConfiguration}
     */
    //@PostConstruct
    public abstract void initialize();

    /**
     * Terminate the {@link PointValueCache}
     */
    @PreDestroy
    public abstract void shutdown();

    /**
     * @return a singleton, thread safe instance of the {@link PointValueCache} implementation
     */
    public abstract PointValueCache getPointValueCache();

}
