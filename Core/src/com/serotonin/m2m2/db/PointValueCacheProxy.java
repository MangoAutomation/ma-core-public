/*
 *
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 * @Author Terry Packer
 *
 */

package com.serotonin.m2m2.db;

import com.github.zafarkhaja.semver.Version;
import com.infiniteautomation.mango.pointvalue.PointValueCacheDao;
import com.serotonin.m2m2.module.ModuleElementDefinition;

/**
 * Interface for proxy manager of persistent access and storage of data point caches.
 */
public abstract class PointValueCacheProxy extends ModuleElementDefinition {

    /**
     * Are we enabled in this configuration? Override as necessary
     * @return
     */
    public boolean isEnabled() {
        return true;
    }

    /**
     * Initialize the proxy and all necessary resources
     */
    protected abstract void initialize();

    /**
     * Terminate the proxy and all necessary resources
     */
    public abstract void shutdown();

    /**
     * Get the dao to access the persistent store
     * @return
     */
    public abstract PointValueCacheDao getDao();

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
