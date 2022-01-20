/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.rt.dataImage.IdPointValueTime;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 * @author Terry Packer
 * @author Jared Wiltshire
 */
public class MockPointValueDao implements PointValueDao {

    protected final ConcurrentMap<Integer, NavigableMap<Long, PointValueTime>> data;

    public MockPointValueDao() {
        this(Map.of());
    }

    public MockPointValueDao(Map<Integer, ? extends Collection<? extends PointValueTime>> data) {
        this.data = new ConcurrentHashMap<>();
        for (var entry : data.entrySet()) {
            this.data.put(entry.getKey(), newSeries(entry.getValue()));
        }
    }

    protected NavigableMap<Long, PointValueTime> newSeries(Collection<? extends PointValueTime> data) {
        TreeMap<Long, PointValueTime> map = new TreeMap<>();
        for (var pvt : data) {
            map.put(pvt.getTime(), pvt);
        }
        return Collections.synchronizedNavigableMap(map);
    }

    protected NavigableMap<Long, PointValueTime> newSeries(int seriesId) {
        return newSeries(List.of());
    }

    @Override
    public PointValueTime savePointValueSync(DataPointVO vo, PointValueTime pointValue) {
        NavigableMap<Long, PointValueTime> values = data.computeIfAbsent(vo.getSeriesId(), this::newSeries);
        values.put(pointValue.getTime(), pointValue);
        return pointValue;
    }

    @Override
    public void savePointValueAsync(DataPointVO vo, PointValueTime pointValue) {
        savePointValueSync(vo, pointValue);
    }

    @Override
    public void getPointValuesPerPoint(Collection<? extends DataPointVO> vos,
                                       @Nullable Long from,
                                       @Nullable Long to,
                                       @Nullable Integer limit,
                                       TimeOrder sortOrder,
                                       Consumer<? super IdPointValueTime> callback) {

        PointValueDao.validateNotNull(vos);
        PointValueDao.validateNotNull(sortOrder);
        PointValueDao.validateNotNull(callback);
        PointValueDao.validateTimePeriod(from, to);
        PointValueDao.validateLimit(limit);

        for (var vo : vos) {
            streamPointValues(vo, from, to, limit, sortOrder).forEachOrdered(callback);
        }
    }

    @Override
    public Stream<IdPointValueTime> streamPointValues(DataPointVO vo,
                                                      @Nullable Long from,
                                                      @Nullable Long to,
                                                      @Nullable Integer limit,
                                                      TimeOrder sortOrder,
                                                      int chunkSize) {

        PointValueDao.validateNotNull(vo);
        PointValueDao.validateTimePeriod(from, to);
        PointValueDao.validateLimit(limit);
        PointValueDao.validateNotNull(sortOrder);
        PointValueDao.validateChunkSize(chunkSize);

        var values = data.getOrDefault(vo.getSeriesId(), Collections.emptyNavigableMap());
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (values) {
            var filteredValues = filterValues(values, from, to);

            if (sortOrder == TimeOrder.DESCENDING) {
                filteredValues = filteredValues.descendingMap();
            }

            var stream = filteredValues.values().stream();
            if (limit != null) {
                stream = stream.limit(limit);
            }
            return stream.map(v -> new IdPointValueTime(vo.getSeriesId(), v.getValue(), v.getTime()));
        }
    }

    @Override
    public Optional<Long> deletePointValuesBetween(DataPointVO vo,
                                                   @Nullable Long startTime,
                                                   @Nullable Long endTime) {

        PointValueDao.validateNotNull(vo);
        PointValueDao.validateTimePeriod(startTime, endTime);

        var values = data.getOrDefault(vo.getSeriesId(), Collections.emptyNavigableMap());
        var filteredValues = filterValues(values, startTime, endTime);
        filteredValues.clear();
        return Optional.empty();
    }

    @Override
    public long dateRangeCount(DataPointVO vo, @Nullable Long from, @Nullable Long to) {
        PointValueDao.validateNotNull(vo);
        PointValueDao.validateTimePeriod(from, to);

        var values = data.getOrDefault(vo.getSeriesId(), Collections.emptyNavigableMap());
        var filteredValues = filterValues(values, from, to);
        return filteredValues.size();
    }

    private @Nullable Long firstKey(NavigableMap<Long, PointValueTime> map) {
        try {
            return map.firstKey();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    @Override
    public Optional<Long> getInceptionDate(DataPointVO vo) {
        PointValueDao.validateNotNull(vo);

        var values = data.getOrDefault(vo.getSeriesId(), Collections.emptyNavigableMap());
        return Optional.ofNullable(firstKey(values));
    }

    @Override
    public Optional<Long> getStartTime(Collection<? extends DataPointVO> vos) {
        PointValueDao.validateNotNull(vos);

        Long startTime = null;

        for (var vo : vos) {
            var values = data.getOrDefault(vo.getSeriesId(), Collections.emptyNavigableMap());
            var value = firstKey(values);
            if (value != null) {
                startTime = startTime == null ? value : Math.min(startTime, value);
            }
        }

        return Optional.ofNullable(startTime);
    }

    @Override
    public Optional<Long> getEndTime(Collection<? extends DataPointVO> vos) {
        PointValueDao.validateNotNull(vos);

        Long endTime = null;

        for (var vo : vos) {
            var values = data.getOrDefault(vo.getSeriesId(), Collections.emptyNavigableMap());
            var value = firstKey(values);
            if (value != null) {
                endTime = endTime == null ? value : Math.max(endTime, value);
            }
        }

        return Optional.ofNullable(endTime);
    }

    private NavigableMap<Long, PointValueTime> filterValues(NavigableMap<Long, PointValueTime> values, Long from, Long to) {
        if (from != null && to != null) {
            return values.subMap(from, true, to, false);
        } else if (from != null) {
            return values.tailMap(from, true);
        } else if (to != null) {
            return values.headMap(to, false);
        }
        return values;
    }

    @Override
    public Optional<Long> deleteOrphanedPointValues() {
        return Optional.empty();
    }

    @Override
    public double writeSpeed() {
        return 0D;
    }

    @Override
    public long queueSize() {
        return 0L;
    }

    @Override
    public int threadCount() {
        return 0;
    }

}
