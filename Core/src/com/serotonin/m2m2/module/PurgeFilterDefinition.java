/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module;

import com.serotonin.m2m2.module.definitions.actions.PurgeFilter;

/**
 * Provides a hook to control the system purge process.
 * 
 * @author Phillip Dunlap
 */
public abstract class PurgeFilterDefinition extends ModuleElementDefinition {
    public abstract PurgeFilter getPurgeFilter();
}
