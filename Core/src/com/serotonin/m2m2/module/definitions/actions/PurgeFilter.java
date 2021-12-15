package com.serotonin.m2m2.module.definitions.actions;

import com.serotonin.m2m2.vo.DataPointVO;

public interface PurgeFilter {
    /**
     * Called as part of the purge process, which runs nightly. This method should return
     * the purgeTime unaltered if it doesn't wish to affect the purge.
     * 
     * @param dpvo
     *            the data point that will be purged
     * @param purgeTime
     *            The time the point is purged unto
     */
    public abstract long adjustPurgeTime(DataPointVO dpvo, long purgeTime);
}
