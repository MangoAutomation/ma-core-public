/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.pointvalue;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 * Access to persistent storage of data point caches.
 */
public interface PointValueCacheDao {

    /**
     * Get the latest values for a data point from the cache, loading from the underlying DAO if needed.
     * Values in the list should be in time descending order (i.e. latest values first).
     *
     * @param vo data point
     * @param size number of point values to be loaded (if cache does not contain values for the point)
     * @return list of latest values (never null), list may not be modifiable
     */
    List<PointValueTime> loadCache(DataPointVO vo, int size);

    /**
     * Update a point's list of latest values. e.g. when a new value is read from a device the value
     * should be prepended to the cache and passed to this function.
     *
     * @param vo data point
     * @param values latest values for the point in time descending order (i.e. latest values first)
     */
    void updateCache(DataPointVO vo, List<PointValueTime> values);

    /**
     * Get the latest values for a set of data points from the cache, loading from the underlying DAO if needed.
     * Values in the lists should be in time descending order (i.e. latest values first).
     *
     * @param vos data points
     * @param size number of point values to be loaded for each point (if cache does not contain values for the point)
     * @return map of datapoint seriesId to list of latest values (never null), lists may not be modifiable
     */
    Map<Integer, List<PointValueTime>> loadCaches(List<DataPointVO> vos, int size);

    /**
     * Get the cache for a point value from the store. Does not load from the underlying DAO, optional will be
     * empty if no values are loaded for the data point.
     *
     * @param vo data point
     * @return optional list of cached point values in time descending order (i.e. latest values first), list may not be modifiable
     */
    Optional<List<PointValueTime>> getCache(DataPointVO vo);

    /**
     * Delete cache for this point, e.g. when point is deleted.
     *
     * @param vo data point
     */
    void deleteCache(DataPointVO vo);

    /**
     * Remove all values for the point, e.g. on purge
     * This should not delete the cache entry, but set it to an empty list.
     *
     * @param vo data point
     */
    default void removeAllValues(DataPointVO vo) {
        updateCache(vo, Collections.emptyList());
    }

    /**
     * Clear the cached value at this time if it exists
     * @param vo data point
     * @param timestamp epoch timestamp, time of value to remove
     */
    void removeValueAt(DataPointVO vo, long timestamp);

    /**
     * Clear cached values before this time (exclusive, a value at this time will not be removed).
     *
     * @param vo data point
     * @param before epoch timestamp, values before this time will be removed
     */
    void removeValuesBefore(DataPointVO vo, long before);

    /**
     * Clear startTime <= values < endTime
     *
     * @param vo data point
     * @param startTime epoch timestamp, values after this time will be removed
     * @param endTime epoch timestamp, values before this time will be removed
     */
    void removeValuesBetween(DataPointVO vo, long startTime, long endTime);
}
