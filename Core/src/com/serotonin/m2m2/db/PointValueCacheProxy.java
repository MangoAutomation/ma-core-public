/*
 *
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 * @Author Terry Packer
 *
 */

package com.serotonin.m2m2.db;

import com.github.zafarkhaja.semver.Version;
import com.serotonin.m2m2.db.dao.PointValueCacheDao;
import com.serotonin.m2m2.module.ModuleElementDefinition;

/**
 * Interface for proxy manager of persistent access and storege of data point caches.
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
     * Get the dao to access the persistsent store
     * @return
     */
    public abstract PointValueCacheDao getDao();

    @Override
    public void postDatabase(Version previousVersion, Version current) {
        initialize();
    }

    @Override
    public void postRuntimeManagerTerminate(boolean uninstall) { shutdown(); }
}
