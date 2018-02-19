/*
    Copyright (C) 2018 Infinite Automation Systems Inc. All rights reserved.
    @author Phillip Dunlap
 */
package com.serotonin.m2m2.module;

import com.serotonin.m2m2.module.definitions.actions.PurgeFilter;

/**
 * Provides a hook to control the system purge process.
 * 
 * @author Phillip Dunlap
 */
public abstract class PurgeFilterDefinition extends ModuleElementDefinition {
    public abstract PurgeFilter newPurgeFilter();
}
