/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 * @Author Terry Packer
 *
 */

package com.serotonin.m2m2.db.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.infiniteautomation.mango.pointvalue.PointValueCacheDao;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 * No-op implementation that reads straight through to the underlying {@link PointValueDao} and does not
 * cache values.
 */
public interface CachingPointValueDao extends PointValueDao, PointValueCacheDao {

    @Override
    default List<PointValueTime> loadCache(DataPointVO vo, int size) {
        if (size == 0) {
            return Collections.emptyList();
        } else if (size == 1) {
            PointValueTime pvt = getLatestPointValue(vo);
            if (pvt != null) {
                return Collections.singletonList(pvt);
            }
            return Collections.emptyList();
        } else {
            return getLatestPointValues(vo, size);
        }
    }

    @Override
    default void updateCache(DataPointVO vo, List<PointValueTime> values) {
        //Do nothing we store this in the point value table with no optimizations yet
    }

    @Override
    default Map<Integer, List<PointValueTime>> loadCaches(List<DataPointVO> vos, int size) {
        Map<Integer, List<PointValueTime>> caches = new HashMap<>(vos.size());
        getLatestPointValues(vos, Long.MAX_VALUE, true, size,
                (pvt, i) -> caches.computeIfAbsent(pvt.getSeriesId(), (k) -> new ArrayList<>(size)).add(pvt));
        return caches;
    }

    @Override
    default Optional<List<PointValueTime>> getCache(DataPointVO vo) {
        return Optional.empty();
    }

    @Override
    default void deleteCache(DataPointVO vo) {
        //No op by default
    }

    @Override
    default void removeValueAt(DataPointVO vo, long timestamp) {
        //No op by default
    }

    @Override
    default void removeValuesBefore(DataPointVO vo, long before) {
        //No op by default
    }

    @Override
    default void removeValuesBetween(DataPointVO vo, long startTime, long endTime) {
        //No op by default
    }

}
