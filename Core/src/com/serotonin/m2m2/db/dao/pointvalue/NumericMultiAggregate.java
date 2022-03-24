/*
 * Copyright (C) 2022 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao.pointvalue;

import java.time.Instant;
import java.util.DoubleSummaryStatistics;

import com.serotonin.m2m2.rt.dataImage.types.DataValue;

/**
 * @author Jared Wiltshire
 */
public class NumericMultiAggregate implements MultiAggregate, NumericAggregate {

    private final long periodStartTime;
    private final long periodEndTime;

    private NumericAggregate firstChild;
    private NumericAggregate lastChild;

    private final DoubleSummaryStatistics statistics = new DoubleSummaryStatistics();
    private Double minimumValue = Double.NaN;
    private Long minimumTime;
    private Double maximumValue = Double.NaN;
    private Long maximumTime;
    private long accumulatedCount = 0;
    private Double averageAccumulator = 0.0D;
    private Double integral = 0.0D;

    public NumericMultiAggregate(Instant periodStartTime, Instant periodEndTime) {
        this(periodStartTime.toEpochMilli(), periodEndTime.toEpochMilli());
    }

    public NumericMultiAggregate(long periodStartTime, long periodEndTime) {
        this.periodStartTime = periodStartTime;
        this.periodEndTime = periodEndTime;
    }

    @Override
    public void accumulate(AggregateValue value) {
        if (!isInPeriod(value)) {
            throw new IllegalArgumentException("Aggregate value does not belong in this period");
        }
        if (!(value instanceof NumericAggregate)) {
            throw new IllegalArgumentException("Must be a instance of " + NumericAggregate.class.getSimpleName());
        }

        NumericAggregate numericValue = (NumericAggregate) value;
        if (firstChild == null) {
            this.firstChild = numericValue;
        }
        this.lastChild = numericValue;
        if (Double.isNaN(minimumValue) || numericValue.getMinimumValue() < minimumValue) {
            this.minimumValue = numericValue.getMinimumValue();
            this.minimumTime = numericValue.getMinimumTime();
        }
        if (Double.isNaN(maximumValue) || numericValue.getMaximumValue() < maximumValue) {
            this.maximumValue = numericValue.getMaximumValue();
            this.maximumTime = numericValue.getMaximumTime();
        }
        this.accumulatedCount++;
        this.averageAccumulator += numericValue.getAverage();
        this.integral += numericValue.getIntegral();
        statistics.combine(numericValue.getStatistics());
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
        return firstChild == null ? null : firstChild.getStartValue();
    }

    @Override
    public DataValue getFirstValue() {
        return firstChild == null ? null : firstChild.getFirstValue();
    }

    @Override
    public Long getFirstTime() {
        return firstChild == null ? null : firstChild.getFirstTime();
    }

    @Override
    public DataValue getLastValue() {
        return lastChild == null ? null : firstChild.getLastValue();
    }

    @Override
    public Long getLastTime() {
        return lastChild == null ? null : firstChild.getLastTime();
    }

    @Override
    public long getCount() {
        return statistics.getCount();
    }

    @Override
    public Double getMinimumValue() {
        return minimumValue;
    }

    @Override
    public Long getMinimumTime() {
        return minimumTime;
    }

    @Override
    public Double getMaximumValue() {
        return maximumValue;
    }

    @Override
    public Long getMaximumTime() {
        return maximumTime;
    }

    @Override
    public Double getAverage() {
        return accumulatedCount == 0L ? Double.NaN : averageAccumulator / accumulatedCount;
    }

    @Override
    public Double getIntegral() {
        return accumulatedCount == 0L ? Double.NaN : integral;
    }

    @Override
    public double getSum() {
        return statistics.getSum();
    }

    @Override
    public double getMinimumInPeriod() {
        return statistics.getCount() > 0L ? statistics.getMin() : Double.NaN;
    }

    @Override
    public double getMaximumInPeriod() {
        return statistics.getCount() > 0L ? statistics.getMax() : Double.NaN;
    }

    @Override
    public DoubleSummaryStatistics getStatistics() {
        return statistics;
    }

    @Override
    public String toString() {
        return "NumericMultiAggregate{" +
                "periodStartTime=" + getPeriodStartInstant() +
                ", periodEndTime=" + getPeriodEndInstant() +
                '}';
    }
}
