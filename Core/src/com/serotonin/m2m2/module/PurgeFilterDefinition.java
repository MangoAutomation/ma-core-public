/*
    Copyright (C) 2018 Infinite Automation Systems Inc. All rights reserved.
    @author Phillip Dunlap
 */
package com.serotonin.m2m2.module;

import com.serotonin.m2m2.vo.DataPointVO;

/**
 * Provides a hook to control the system purge process.
 * 
 * @author Phillip Dunlap
 */
public abstract class PurgeFilterDefinition extends ModuleElementDefinition {
    /**
     * Called as part of the purge process, which runs nightly. This method should return
     * the purgeTime unaltered if it doesn't wish to affect the purge.
     * 
     * @param dataPointId
     *            The ID of the data point that will be purged
     * @param purgeTime
     *            The time the point is purged unto
     */
    public abstract long adjustPurgeTime(DataPointVO dpvo, long purgeTime);
}
