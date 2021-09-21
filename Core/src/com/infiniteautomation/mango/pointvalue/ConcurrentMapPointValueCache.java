/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.pointvalue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 * Implementation of a {@link PointValueCacheDao} backed by a {@link ConcurrentMap}.
 */
public class ConcurrentMapPointValueCache implements PointValueCacheDao {

    private final ConcurrentMap<Integer, List<PointValueTime>> cache;
    private final PointValueDao pointValueDao;

    public ConcurrentMapPointValueCache(ConcurrentMap<Integer, List<PointValueTime>> cache, PointValueDao pointValueDao) {
        this.cache = cache;
        this.pointValueDao = pointValueDao;
    }

    @Override
    public List<PointValueTime> expandPointValueCache(DataPointVO vo, int newSize, List<PointValueTime> currentCache) {
        if (currentCache.size() >= newSize) return currentCache;

        return this.cache.compute(vo.getSeriesId(), (k, v) -> {
            if (v == null || v.size() < newSize) {
                v = pointValueDao.getLatestPointValues(vo, newSize);
            }
            return v;
        });
    }

    @Override
    public void updatePointValueCache(DataPointVO vo, List<PointValueTime> values) {
        this.cache.put(vo.getSeriesId(), values);
    }

    @Override
    public Map<Integer, List<PointValueTime>> getPointValueCaches(List<DataPointVO> vos, Integer size) {
        Map<Integer, List<PointValueTime>> result = new HashMap<>(vos.size());
        List<DataPointVO> missingPoints = new ArrayList<>();

        for (DataPointVO vo : vos) {
            List<PointValueTime> values = cache.get(vo.getSeriesId());
            if (values == null) {
                // find which points are missing from the cache
                missingPoints.add(vo);
                result.put(vo.getSeriesId(), Collections.emptyList());
            } else {
                result.put(vo.getSeriesId(), values);
            }
        }

        // need to load point values from time series database and store in cache
        if (!missingPoints.isEmpty()) {
            Map<Integer, List<PointValueTime>> loaded = loadFromDatabase(missingPoints);

            // put the intermediate map into the result and into our cache
            cache.putAll(loaded);
            result.putAll(loaded);
        }

        return result;
    }

    private Map<Integer, List<PointValueTime>> loadFromDatabase(List<DataPointVO> points) {
        Map<Integer, DataPointVO> pointsBySeries = points.stream()
                .collect(Collectors.toMap(DataPointVO::getSeriesId, Function.identity()));

        int queryLimit = points.stream().mapToInt(DataPointVO::getDefaultCacheSize)
                .max().orElse(0);

        // fetch the point values from the database and store in map
        Map<Integer, List<PointValueTime>> result = new HashMap<>(points.size());
        pointValueDao.getLatestPointValues(points, Long.MAX_VALUE, true, queryLimit, (pvt, i) -> {
            List<PointValueTime> values = result.computeIfAbsent(pvt.getSeriesId(), k -> new ArrayList<>());
            DataPointVO point = pointsBySeries.get(pvt.getSeriesId());

            // we may retrieve up to maxCacheSize values, however dont store more than the point's cache size
            if (values.size() < point.getDefaultCacheSize()) {
                values.add(pvt);
            }
        });

        return result;
    }

    @Override
    public List<PointValueTime> getPointValueCache(DataPointVO vo) {
        return cache.getOrDefault(vo.getSeriesId(), Collections.emptyList());
    }

    @Override
    public void deleteAllCaches() {
        this.cache.clear();
    }

    @Override
    public void deleteCache(DataPointVO vo) {
        cache.remove(vo.getSeriesId());
    }

    @Override
    public void deleteCachedValue(DataPointVO vo, long timestamp) {
        removeValues(vo, value -> value.getTime() == timestamp);
    }

    @Override
    public void deleteCachedValuesBefore(DataPointVO vo, long before) {
        removeValues(vo, value -> value.getTime() < before);
    }

    @Override
    public void deleteCachedValuesBetween(DataPointVO vo, long startTime, long endTime) {
        removeValues(vo, value -> value.getTime() >= startTime && value.getTime() < endTime);
    }

    private void removeValues(DataPointVO vo, Predicate<PointValueTime> predicate) {
        this.cache.computeIfPresent(vo.getSeriesId(), (k, v) -> {
            if (v.isEmpty()) return v;
            ArrayList<PointValueTime> newList = v instanceof ArrayList ? (ArrayList<PointValueTime>) v : new ArrayList<>(v);
            newList.removeIf(predicate);
            return newList;
        });
    }
}
