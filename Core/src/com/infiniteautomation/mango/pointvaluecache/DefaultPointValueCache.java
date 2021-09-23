/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.pointvaluecache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 * No-op implementation that reads straight through to the underlying {@link PointValueDao} and does not
 * cache values.
 */
public class DefaultPointValueCache implements PointValueCache {

    private final PointValueDao pointValueDao;

    public DefaultPointValueCache(PointValueDao pointValueDao) {
        this.pointValueDao = pointValueDao;
    }

    @Override
    public List<PointValueTime> loadCache(DataPointVO vo, int size) {
        if (size == 0) {
            return Collections.emptyList();
        } else if (size == 1) {
            PointValueTime pvt = pointValueDao.getLatestPointValue(vo);
            if (pvt != null) {
                return Collections.singletonList(pvt);
            }
            return Collections.emptyList();
        } else {
            return pointValueDao.getLatestPointValues(vo, size);
        }
    }

    @Override
    public void updateCache(DataPointVO vo, List<PointValueTime> values) {
        // no-op
    }

    @Override
    public Map<Integer, List<PointValueTime>> loadCaches(List<DataPointVO> vos, int size) {
        Map<Integer, List<PointValueTime>> caches = new HashMap<>(vos.size());
        pointValueDao.getLatestPointValues(vos, Long.MAX_VALUE, true, size,
                (pvt, i) -> caches.computeIfAbsent(pvt.getSeriesId(), (k) -> new ArrayList<>(size)).add(pvt));
        return caches;
    }

    @Override
    public Optional<List<PointValueTime>> getCache(DataPointVO vo) {
        return Optional.empty();
    }

    @Override
    public void deleteCache(DataPointVO vo) {
        // no-op
    }

    @Override
    public void removeValueAt(DataPointVO vo, long timestamp) {
        // no-op
    }

    @Override
    public void removeValuesBefore(DataPointVO vo, long before) {
        // no-op
    }

    @Override
    public void removeValuesBetween(DataPointVO vo, long startTime, long endTime) {
        // no-op
    }

}
