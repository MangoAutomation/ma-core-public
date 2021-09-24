/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module;

import org.springframework.beans.factory.annotation.Autowired;

import com.serotonin.m2m2.db.dao.SystemSettingsDao;

/**
 * Provides a hook into the system purge process.
 * 
 * @author Matthew Lohbihler
 */
abstract public class PurgeDefinition extends ModuleElementDefinition {

    @Autowired
    protected SystemSettingsDao systemSettingsDao;

    /**
     * Called as part of the purge process, which runs nightly.
     * 
     * @param runtime
     *            The time at which the purge process started. May differ significantly from the current time.
     */
    abstract public void execute(long runtime);
}
