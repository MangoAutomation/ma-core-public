/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao;

import java.time.Period;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.serotonin.m2m2.db.dao.pointvalue.TimeOrder;
import com.serotonin.m2m2.rt.dataImage.IdPointValueTime;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.vo.DataPointVO;

public abstract class DelegatingPointValueDao implements PointValueDao {

    protected final PointValueDao primary;
    protected final PointValueDao secondary;

    public DelegatingPointValueDao(PointValueDao primary, PointValueDao secondary) {
        this.primary = primary;
        this.secondary = secondary;
    }

    public enum Operation {
        READ,
        WRITE,
        DELETE
    }

    public abstract boolean handleWithPrimary(Operation operation);

    public boolean handleWithPrimary(DataPointVO vo, Operation operation) {
        return handleWithPrimary(operation);
    }

    @Override
    public PointValueTime savePointValueSync(DataPointVO vo, PointValueTime pointValue) {
        if (handleWithPrimary(vo, Operation.WRITE)) {
            return primary.savePointValueSync(vo, pointValue);
        } else {
            return secondary.savePointValueSync(vo, pointValue);
        }
    }

    @Override
    public void savePointValueAsync(DataPointVO vo, PointValueTime pointValue) {
        if (handleWithPrimary(vo, Operation.WRITE)) {
            primary.savePointValueAsync(vo, pointValue);
        } else {
            secondary.savePointValueAsync(vo, pointValue);
        }
    }

    @Override
    public void getPointValuesPerPoint(Collection<? extends DataPointVO> vos, @Nullable Long from, @Nullable Long to,
                                       @Nullable Integer limit, TimeOrder sortOrder, Consumer<? super IdPointValueTime> callback) {

        Map<Boolean, ? extends List<? extends DataPointVO>> dataPoints = vos.stream()
                .collect(Collectors.partitioningBy(vo -> handleWithPrimary(vo, Operation.READ)));

        primary.getPointValuesPerPoint(dataPoints.get(true), from, to, limit, sortOrder, callback);
        secondary.getPointValuesPerPoint(dataPoints.get(false), from, to, limit, sortOrder, callback);
    }

    @Override
    public void getPointValuesCombined(Collection<? extends DataPointVO> vos, @Nullable Long from, @Nullable Long to,
                                       @Nullable Integer limit, TimeOrder sortOrder, Consumer<? super IdPointValueTime> callback) {

        Map<Boolean, ? extends List<? extends DataPointVO>> dataPoints = vos.stream()
                .collect(Collectors.partitioningBy(vo -> handleWithPrimary(vo, Operation.READ)));

        List<? extends DataPointVO> primaryPoints = dataPoints.get(true);
        List<? extends DataPointVO> secondaryPoints = dataPoints.get(false);

        if (secondaryPoints.isEmpty()) {
            primary.getPointValuesCombined(primaryPoints, from, to, limit, sortOrder, callback);
        } else if (primaryPoints.isEmpty()) {
            secondary.getPointValuesCombined(secondaryPoints, from, to, limit, sortOrder, callback);
        } else {
            // use the default method which get can combine multiple streams without loading into memory
            PointValueDao.super.getPointValuesCombined(vos, from, to, limit, sortOrder, callback);
        }
    }

    @Override
    public boolean enablePerPointPurge() {
        return primary.enablePerPointPurge() || secondary.enablePerPointPurge();
    }

    @Override
    public void setRetentionPolicy(Period period) {
        UnsupportedOperationException firstException = null;
        try {
            primary.setRetentionPolicy(period);
        } catch (UnsupportedOperationException e) {
            firstException = e;
        }

        try {
            secondary.setRetentionPolicy(period);
        } catch (UnsupportedOperationException e) {
            // only throw if both primary and secondary failed
            if (firstException != null) {
                firstException.addSuppressed(e);
                throw firstException;
            }
        }
    }

    @Override
    public long dateRangeCount(DataPointVO vo, @Nullable Long from, @Nullable Long to) {
        if (handleWithPrimary(vo, Operation.READ)) {
            return primary.dateRangeCount(vo, from, to);
        } else {
            return secondary.dateRangeCount(vo, from, to);
        }
    }

    @Override
    public Optional<Long> deletePointValuesBetween(DataPointVO vo, @Nullable Long startTime, @Nullable Long endTime) {
        if (handleWithPrimary(vo, Operation.DELETE)) {
            primary.deletePointValuesBetween(vo, startTime, endTime);
        } else {
            secondary.deletePointValuesBetween(vo, startTime, endTime);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Long> deleteAllPointData() {
        if (handleWithPrimary(Operation.DELETE)) {
            primary.deleteAllPointData();
        }
        secondary.deleteAllPointData();
        return Optional.empty();
    }

    @Override
    public Optional<Long> deleteOrphanedPointValues() {
        if (handleWithPrimary(Operation.DELETE)) {
            primary.deleteOrphanedPointValues();
        }
        secondary.deleteOrphanedPointValues();
        return Optional.empty();
    }

    @Override
    public double writeSpeed() {
        return primary.writeSpeed() + secondary.writeSpeed();
    }

    @Override
    public long queueSize() {
        return primary.queueSize() + secondary.queueSize();
    }

    @Override
    public int threadCount() {
        return primary.threadCount() + secondary.threadCount();
    }

}
