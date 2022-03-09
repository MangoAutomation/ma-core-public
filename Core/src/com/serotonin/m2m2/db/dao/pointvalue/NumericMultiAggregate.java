/*
 * Copyright (C) 2022 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao.pointvalue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.serotonin.m2m2.rt.dataImage.types.DataValue;

/**
 * @author Jared Wiltshire
 */
public class NumericMultiAggregate implements MultiAggregate, NumericAggregate {

    private final long periodStartTime;
    private final long periodEndTime;
    private final List<NumericAggregate> children = new ArrayList<>(0);

    public NumericMultiAggregate(Instant periodStartTime, Instant periodEndTime) {
        this(periodStartTime.toEpochMilli(), periodEndTime.toEpochMilli());
    }

    public NumericMultiAggregate(long periodStartTime, long periodEndTime) {
        this.periodStartTime = periodStartTime;
        this.periodEndTime = periodEndTime;
    }

    @Override
    public void addChild(AggregateValue child) {
        if (!isInPeriod(child)) {
            throw new IllegalArgumentException("Aggregate does not belong in this period");
        }
        if (!(child instanceof NumericAggregate)) {
            throw new IllegalArgumentException("Must be a NumericAggregate");
        }
        children.add((NumericAggregate) child);
    }

    private NumericAggregate firstChild() {
        return children.get(0);
    }

    private NumericAggregate lastChild() {
        return children.get(children.size() - 1);
    }

    public Instant getPeriodStartInstant() {
        return Instant.ofEpochMilli(periodStartTime);
    }

    public Instant getPeriodEndInstant() {
        return Instant.ofEpochMilli(periodEndTime);
    }

    @Override
    public long getPeriodStartTime() {
        return periodStartTime;
    }

    @Override
    public long getPeriodEndTime() {
        return periodEndTime;
    }

    @Override
    public DataValue getStartValue() {
        return children.isEmpty() ? null : firstChild().getStartValue();
    }

    @Override
    public DataValue getFirstValue() {
        return children.isEmpty() ? null : firstChild().getFirstValue();
    }

    @Override
    public Long getFirstTime() {
        return children.isEmpty() ? null : firstChild().getFirstTime();
    }

    @Override
    public DataValue getLastValue() {
        return children.isEmpty() ? null : lastChild().getLastValue();
    }

    @Override
    public Long getLastTime() {
        return children.isEmpty() ? null : lastChild().getLastTime();
    }

    @Override
    public long getCount() {
        return children.stream()
                .mapToLong(NumericAggregate::getCount)
                .sum();
    }

    @Override
    public Double getMinimumValue() {
        return children.stream()
                .mapToDouble(NumericAggregate::getMinimumValue)
                .min()
                .orElse(Double.NaN);
    }

    @Override
    public Long getMinimumTime() {
        double minimum = getMinimumValue();
        return children.stream()
                .filter(v -> v.getMinimumValue() == minimum)
                .map(NumericAggregate::getMinimumTime)
                .findAny()
                .orElse(null);
    }

    @Override
    public Double getMaximumValue() {
        return children.stream()
                .mapToDouble(NumericAggregate::getMaximumValue)
                .min()
                .orElse(Double.NaN);
    }

    @Override
    public Long getMaximumTime() {
        double maximum = getMaximumValue();
        return children.stream()
                .filter(v -> v.getMaximumValue() == maximum)
                .map(NumericAggregate::getMinimumTime)
                .findAny()
                .orElse(null);
    }

    @Override
    public Double getAverage() {
        // will not return a true time weighted average unless child periods are perfectly aligned
        return children.stream()
                .mapToDouble(NumericAggregate::getAverage)
                .average()
                .orElse(Double.NaN);
    }

    @Override
    public Double getIntegral() {
        return children.stream()
                .mapToDouble(NumericAggregate::getIntegral)
                .sum();
    }

    @Override
    public double getSum() {
        return children.stream()
                .mapToDouble(NumericAggregate::getSum)
                .sum();
    }

    @Override
    public double getMinimumInPeriod() {
        return children.stream()
                .mapToDouble(NumericAggregate::getMinimumInPeriod)
                .min()
                .orElse(Double.NaN);
    }

    @Override
    public double getMaximumInPeriod() {
        return children.stream()
                .mapToDouble(NumericAggregate::getMaximumInPeriod)
                .max()
                .orElse(Double.NaN);
    }

    @Override
    public String toString() {
        return "NumericMultiAggregate{" +
                "periodStartTime=" + getPeriodStartInstant() +
                ", periodEndTime=" + getPeriodEndInstant() +
                ", children=" + children.size() +
                '}';
    }
}
