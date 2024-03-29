/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.statistics;

import java.time.Instant;
import java.util.DoubleSummaryStatistics;
import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.db.dao.pointvalue.NumericAggregate;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.view.stats.IValueTime;
import com.serotonin.m2m2.view.stats.StatisticsGenerator;

/**
 * Enhanced statistics using Rollups Discussion document
 *
 * @author Matthew Lohbihler, Terry Packer
 */
public class AnalogStatistics implements StatisticsGenerator, NumericAggregate {

    // Configuration values.
    private final long periodStart;
    private final long periodEnd;
    private boolean done = false;

    // Calculated values.
    private Double minimumValue = Double.NaN;
    private Long minimumTime;
    private Double maximumValue = Double.NaN;
    private Long maximumTime;
    private Double average = Double.NaN;
    private Double integral = Double.NaN;
    private DataValue firstValue;
    private Long firstTime;
    private DataValue lastValue;
    private Long lastTime;
    private DataValue startValue;
    private Double delta = Double.NaN;

    // State values used for calculating weighted average.
    private Double latestValue;
    private long latestTime;
    private long totalDuration;

    private final DoubleSummaryStatistics statistics = new DoubleSummaryStatistics();

    public AnalogStatistics(long periodStart, long periodEnd, IValueTime<DataValue> startVT,
            List<? extends IValueTime<DataValue>> values) {
        this(periodStart, periodEnd, startVT);
        for (IValueTime<DataValue> p : values)
            addValueTime(p);
        done();
    }

    /**
     * @param periodStart start of period (epoch ms)
     * @param periodEnd end of period (epoch ms)
     * @param startValue may be null when used for interval logging, see {@link com.serotonin.m2m2.rt.dataImage.DataPointRT}
     */
    public AnalogStatistics(long periodStart, long periodEnd, @Nullable IValueTime<DataValue> startValue) {
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;

        //Check for null and also bookend values
        if (startValue != null && startValue.getValue() != null) {
            this.startValue = startValue.getValue();
            minimumValue = maximumValue = latestValue = this.startValue.getDoubleValue();
            minimumTime = maximumTime = latestTime = periodStart;
        }
    }


    @Override
    public void addValueTime(IValueTime<DataValue> vt) {
        addValueTime(vt.getValue(), vt.getTime());
    }

    public void addValueTime(DataValue value, long time) {
        if (value == null)
            return;

        double doubleValue = value.getDoubleValue();

        if (firstValue == null) {
            firstValue = value;
            firstTime = time;
        }

        if (minimumValue.isNaN() || minimumValue > doubleValue) {
            minimumValue = doubleValue;
            minimumTime = time;
        }

        if (maximumValue.isNaN() || maximumValue < doubleValue) {
            maximumValue = doubleValue;
            maximumTime = time;
        }

        updateAverage(doubleValue, time);

        lastValue = value;
        lastTime = time;

        statistics.accept(doubleValue);
    }

    @Override
    public void done() {
        if(done)
            throw new ShouldNeverHappenException("Should not call done() more than once.");
        done = true;

        updateAverage(Double.NaN, periodEnd);
        // Average will not be NaN when we have at least one value in period AND an end value
        // OR more than 1 value in the period
        if (!average.isNaN()) {
            integral = average / 1000D; // integrate over seconds not msecs
            average /= totalDuration;
        } else {
            // Special case: if there was no start value and no end value, and only one value in the
            // data set, we will
            // have a latest value, and a duration of zero. For this value we set the average equal
            // to that value.
            if(lastValue != null) {
                average = lastValue.getDoubleValue();
                // Nothing to integrate
                integral = 0D;
            }
        }

        if (firstValue != null) {
            if(startValue != null)
                delta = lastValue.getDoubleValue() - startValue.getDoubleValue();
            else
                delta = lastValue.getDoubleValue() - firstValue.getDoubleValue();
        }else if(startValue != null)
            delta = 0.0D; //No data but a start value
    }

    private void updateAverage(double value, long time) {
        if (latestValue != null) {
            // The duration for which the last value was in force.
            long duration = time - latestTime;

            if (duration > 0) {
                // Determine the weighted average of the latest value. The average value at this
                // point still needs to
                // be divided by the total duration of the period.
                if (average.isNaN())
                    average = 0D;
                average = average + ( latestValue * duration);
                totalDuration += duration;
            }
        }

        // Reset the latest value.
        latestValue = value;
        latestTime = time;
    }

    @Override
    public long getPeriodStartTime() {
        return periodStart;
    }

    @Override
    public long getPeriodEndTime() {
        return periodEnd;
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
        return average;
    }

    @Override
    public Double getIntegral() {
        return integral;
    }

    @Override
    public double getSum() {
        return statistics.getSum();
    }

    @Override
    public DataValue getStartValue() {
        return startValue;
    }

    @Override
    public DataValue getFirstValue() {
        return firstValue;
    }

    @Override
    public Long getFirstTime() {
        return firstTime;
    }

    @Override
    public DataValue getLastValue() {
        return lastValue;
    }

    @Override
    public Long getLastTime() {
        return lastTime;
    }

    @Override
    public long getCount() {
        return statistics.getCount();
    }

    @Override
    public double getDelta() {
        return this.delta;
    }

    public String getHelp() {
        return toString();
    }

    @Override
    public double getMinimumInPeriod() {
        return getCount() > 0 ? statistics.getMin() : Double.NaN;
    }

    @Override
    public double getMaximumInPeriod() {
        return getCount() > 0 ? statistics.getMax() : Double.NaN;
    }

    @Override
    public DoubleSummaryStatistics getStatistics() {
        return statistics;
    }

    @Override
    public String toString() {
        return "{minimumValue: " + minimumValue +
                ", minimumTime: " + formatEpochMilli(minimumTime) +
                ", maximumValue: " + maximumValue +
                ", maximumTime: " + formatEpochMilli(maximumTime) +
                ", average: " + average +
                ", sum: " + getSum() +
                ", count: " + getCount() +
                ", delta: " + delta +
                ", integral: " + integral +
                ", startValue: " + startValue +
                ", firstValue: " + firstValue +
                ", firstTime: " + formatEpochMilli(firstTime) +
                ", lastValue: " + lastValue +
                ", lastTime: " + formatEpochMilli(lastTime) +
                ", periodStartTime: " + formatEpochMilli(periodStart)
                + ", periodEndTime: " + formatEpochMilli(periodEnd) + "}";
    }

    private String formatEpochMilli(Long time) {
        return time == null ? "null" : Instant.ofEpochMilli(time).toString();
    }
}
