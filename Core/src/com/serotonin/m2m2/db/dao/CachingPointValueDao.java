/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 * @Author Terry Packer
 *
 */

package com.serotonin.m2m2.db.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.infiniteautomation.mango.pointvalue.PointValueCacheDao;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.vo.DataPointVO;

public interface CachingPointValueDao extends PointValueDao, PointValueCacheDao {

    @Override
    public default List<PointValueTime> expandPointValueCache(DataPointVO vo, int size, List<PointValueTime> existing) {
        if (size == 1) {
            PointValueTime pvt = getLatestPointValue(vo);
            if (pvt != null) {
                List<PointValueTime> c = new ArrayList<PointValueTime>();
                c.add(pvt);
                return c;
            } else {
                return existing;
            }
        } else {
            List<DataPointVO> vos = new ArrayList<>();
            vos.add(vo);
            List<PointValueTime> cc = new ArrayList<>();
            cc.addAll(existing);
            List<PointValueTime> nc = new ArrayList<>(size);
            List<PointValueTime> fromDb = getLatestPointValues(vo, size);
            for(PointValueTime value : fromDb) {
                //Cache is in same order as rows
                if (nc.size() < size && cc.size() > 0 && cc.get(0).getTime() >= value.getTime()) {
                    //The cached value is newer so add it
                    while (nc.size() < size && cc.size() > 0 && cc.get(0).getTime() > value.getTime())
                        nc.add(cc.remove(0));
                    if (cc.size() > 0 && cc.get(0).getTime() == value.getTime())
                        cc.remove(0);
                    if (nc.size() < size)
                        nc.add(value);
                } else {
                    //Past cached value times
                    if (nc.size() < size)
                        nc.add(value);
                }
            }
            return nc;
        }
    }

    @Override
    public default void updatePointValueCache(DataPointVO vo, List<PointValueTime> values) {
        //Do nothing we store this in the point value table with no optimizations yet
    }

    @Override
    public default Map<Integer, List<PointValueTime>> getPointValueCaches(List<DataPointVO> vos, Integer size) {
        Map<Integer, List<PointValueTime>> caches = new HashMap<>(vos.size());
        getLatestPointValues(vos, Long.MAX_VALUE, true, size,
                (pvt, i) -> caches.computeIfAbsent(pvt.getSeriesId(), (k) -> new ArrayList<>()).add(pvt));
        return caches;
    }

    @Override
    public default List<PointValueTime> getPointValueCache(DataPointVO vo) {
        List<PointValueTime> cache = new ArrayList<>();
        return getLatestPointValues(vo, vo.getDefaultCacheSize());
    }

    /**
     * Clear all caches, for example after a full data purge
     */
    @Override
    public default void deleteAllCaches() {
        //No op by default
    }

    /**
     * Clear cache for this point
     * @param vo
     */
    @Override
    public default void deleteCache(DataPointVO vo) {
        //No op by default
    }

    /**
     * Delete the cached value at this time if it exists
     * @param vo
     * @param timestamp
     */
    @Override
    public default void deleteCachedValue(DataPointVO vo, long timestamp) {
        //No op by default
    }


    /**
     * Delete cached values before this time
     * @param vo
     * @param before
     */
    @Override
    public default void deleteCachedValuesBefore(DataPointVO vo, long before) {
        //No op by default
    }

    /**
     * Delete startTime <= values < endTime
     *
     * @param vo
     * @param startTime
     * @param endTime
     */
    @Override
    public default void deleteCachedValuesBetween(DataPointVO vo, long startTime, long endTime) {
        //No op by default
    }

}
