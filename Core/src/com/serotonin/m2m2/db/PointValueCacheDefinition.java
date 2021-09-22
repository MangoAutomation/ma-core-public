/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db;

import com.github.zafarkhaja.semver.Version;
import com.infiniteautomation.mango.pointvalue.PointValueCacheDao;
import com.serotonin.m2m2.module.ModuleElementDefinition;

/**
 * Definition that provides a {@link PointValueCacheDao} for accessing point value caches.
 */
public abstract class PointValueCacheDefinition extends ModuleElementDefinition {

    /**
     * @return true if enabled
     */
    public boolean isEnabled() {
        return true;
    }

    /**
     * Initialize the {@link PointValueCacheDao}
     */
    protected abstract void initialize();

    /**
     * Terminate the {@link PointValueCacheDao}
     */
    public abstract void shutdown();

    /**
     * @return a singleton, thread safe instance of the {@link PointValueCacheDao} implementation
     */
    public abstract PointValueCacheDao getPointValueCache();

    @Override
    public void postDatabase(Version previousVersion, Version current) {
        if (isEnabled()) {
            initialize();
        }
    }

    @Override
    public void postRuntimeManagerTerminate(boolean uninstall) {
        if (isEnabled()) {
            shutdown();
        }
    }
}
