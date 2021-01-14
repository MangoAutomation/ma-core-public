/*
 *
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 * @Author Terry Packer
 *
 */

package com.serotonin.m2m2.db;

import com.github.zafarkhaja.semver.Version;
import com.serotonin.m2m2.db.dao.LatestPointValueDao;
import com.serotonin.m2m2.module.ModuleElementDefinition;

public abstract class LatestPointValueProxy extends ModuleElementDefinition {

    /**
     * Are we enabled in this configuration? Override as necessary
     * @return
     */
    public boolean isEnabled() {
        return true;
    }

    protected abstract void initialize();

    public abstract void shutdown();

    public abstract LatestPointValueDao getDao();

    @Override
    public void postDatabase(Version previousVersion, Version current) {
        initialize();
    }
}
