/*
 *
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 * @Author Terry Packer
 *
 */

package com.serotonin.m2m2.db.dao;

import java.util.List;
import java.util.Map;

import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.vo.DataPointVO;
import com.sun.istack.NotNull;

public interface LatestPointValueDao {

    /**
     * Merge the cache for a data point in time descending order
     * @param vo
     * @param size - size of list to return
     * @param existing - current cache (DO NOT MODIFY)
     * @return - values or empty list never null
     */
    public @NotNull List<PointValueTime> expandCachedPointValues(DataPointVO vo, int size, List<PointValueTime> existing);

    /**
     * Update the list of latest point values, should represent the current point's cache and
     * be in time descending order
     * @param vo
     * @param values
     */
    public void updateLatestPointValues(DataPointVO vo, List<PointValueTime> values);

    /**
     * Get the latest values for a group of data points in time descending order
     *
     * @param vos
     * @param size - size of all lists
     * @return - Map of Ids to list where list is never null
     */
    public @NotNull Map<Integer, List<PointValueTime>> getLatestPointValues(List<DataPointVO> vos, int size);
}
