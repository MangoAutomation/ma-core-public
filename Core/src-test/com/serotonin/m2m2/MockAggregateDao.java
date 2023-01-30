/*
 * Copyright (C) 2022 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.util.Collections;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.db.dao.pointvalue.AggregateValue;
import com.serotonin.m2m2.db.dao.pointvalue.BoundaryAggregateDao;
import com.serotonin.m2m2.view.stats.DefaultSeriesValueTime;
import com.serotonin.m2m2.view.stats.IValueTime;
import com.serotonin.m2m2.view.stats.SeriesValueTime;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 * @author Jared Wiltshire
 */
public class MockAggregateDao implements BoundaryAggregateDao {

    protected final PointValueDao pointValueDao;
    protected final Clock clock;
    protected final TemporalAmount boundary;
    protected final TemporalAmount preAggregationPeriod;
    protected final ConcurrentMap<Integer, NavigableMap<Long, IValueTime<? extends AggregateValue>>> data;

    public MockAggregateDao(PointValueDao pointValueDao, Clock clock, TemporalAmount preAggregationPeriod, TemporalAmount boundary) {
        this.pointValueDao = pointValueDao;
        this.clock = clock;
        this.boundary = boundary;
        this.preAggregationPeriod = preAggregationPeriod;
        this.data = new ConcurrentHashMap<>();
    }

    protected NavigableMap<Long, IValueTime<? extends AggregateValue>> newSeries(int seriesId) {
        return Collections.synchronizedNavigableMap(new TreeMap<>());
    }

    @Override
    public PointValueDao getPointValueDao() {
        return pointValueDao;
    }

    @Override
    public ZonedDateTime boundary() {
        return ZonedDateTime.now(clock)
                .minus(boundary);
    }

    @Override
    public long fromBoundary(TemporalUnit unit) {
        var now = ZonedDateTime.now(clock);
        var queryBoundary = now.minus(boundary);
        return unit.between(queryBoundary, now);
    }

    @Override
    public TemporalAmount preAggregationPeriod() {
        return preAggregationPeriod;
    }

    @Override
    public boolean preAggregationSupported(DataPointVO point) {
        return true;
    }

    @Override
    public Stream<SeriesValueTime<AggregateValue>> queryPreAggregated(DataPointVO point, ZonedDateTime from, ZonedDateTime to) {
        var values = data.getOrDefault(point.getSeriesId(), Collections.emptyNavigableMap());
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (values) {
            var filteredValues = values
                    .subMap(from.toInstant().toEpochMilli(), true, to.toInstant().toEpochMilli(), false);
            return filteredValues.values().stream()
                    .map(v -> new DefaultSeriesValueTime<>(point.getSeriesId(), v.getTime(), v.getValue()));
        }
    }

    @Override
    public void save(DataPointVO point, Stream<? extends IValueTime<? extends AggregateValue>> aggregates, int chunkSize) {
        var values = data.computeIfAbsent(point.getSeriesId(), this::newSeries);
        synchronized (values) {
            aggregates.forEach(v -> values.put(v.getTime(), v));
        }
    }
}
