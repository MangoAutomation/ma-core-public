/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.pointvalue;

import java.util.List;
import java.util.Map;

import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.vo.DataPointVO;


/**
 * Access to persistent storage of data point caches.
 */
public interface PointValueCacheDao {

    /**
     * Expand the cache for a data point, get new values from the point value dao and merge them in time descending order
     * @param vo
     * @param size - size of list to return
     * @param existing - current cache (DO NOT MODIFY)
     * @return - values or empty list never null
     */
    public List<PointValueTime> expandPointValueCache(DataPointVO vo, int size, List<PointValueTime> existing);

    /**
     * Update the list of latest point values, should represent the current point's cache and
     * be in time descending order
     * @param vo
     * @param values
     */
    public void updatePointValueCache(DataPointVO vo, List<PointValueTime> values);

    /**
     * Get the latest values for a group of data points in time descending order
     *
     * @param vos
     * @param size - size of all lists, may be ignored if store contains only the cache store.  If overlaid onto
     *             a PointValueDao then this is required and will be used.
     * @return - Map of vo.seriesId to list where list is never null
     */
    public Map<Integer, List<PointValueTime>> getPointValueCaches(List<DataPointVO> vos, Integer size);

    /**
     * Get the cache for a point value from the store, may be empty but never null
     * @param vo
     * @return
     */
    public List<PointValueTime> getPointValueCache(DataPointVO vo);

    /**
     * Clear all caches, for example after a full data purge
     */
    public void deleteAllCaches();

    /**
     * Clear cache for this point
     * @param vo
     */
    public void deleteCache(DataPointVO vo);

    /**
     * Delete the cached value at this time if it exists
     * @param vo
     * @param timestamp
     */
    public void deleteCachedValue(DataPointVO vo, long timestamp);


    /**
     * Delete cached values before this time
     * @param vo
     * @param before
     */
    void deleteCachedValuesBefore(DataPointVO vo, long before);

    /**
     * Delete startTime <= values < endTime
     *
     * @param vo
     * @param startTime
     * @param endTime
     */
    void deleteCachedValuesBetween(DataPointVO vo, long startTime, long endTime);
}
