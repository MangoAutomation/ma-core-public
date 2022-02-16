/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.statistics;

import java.util.DoubleSummaryStatistics;
import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.db.dao.pointvalue.NumericAggregate;
import com.serotonin.m2m2.rt.dataImage.IdPointValueTime;
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
    private Double firstValue;
    private Long firstTime;
    private Double lastValue;
    private Long lastTime;
    private Double startValue;
    private Double delta = Double.NaN;

    // State values used for calculating weighted average.
    private Double latestValue;
    private long latestTime;
    private long totalDuration;

    private final DoubleSummaryStatistics statistics = new DoubleSummaryStatistics();
    private final int seriesId;

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

        this.seriesId = startValue instanceof IdPointValueTime ?
                ((IdPointValueTime) startValue).getSeriesId() :
                -1;

        //Check for null and also bookend values
        if (startValue != null && startValue.getValue() != null) {
            minimumValue = maximumValue = latestValue = this.startValue = startValue.getValue().getDoubleValue();
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
            firstValue = doubleValue;
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

        lastValue = value.getDoubleValue();
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
                average = lastValue;
                // Nothing to integrate
                integral = 0D;
            }
        }

        if (firstValue != null) {
            if(startValue != null)
                delta = lastValue - startValue;
            else
                delta = lastValue - firstValue;
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
    public Double getStartValue() {
        return startValue;
    }

    @Override
    public Double getFirstValue() {
        return firstValue;
    }

    @Override
    public Long getFirstTime() {
        return firstTime;
    }

    @Override
    public Double getLastValue() {
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
    public double getArithmeticMean() {
        return statistics.getAverage();
    }

    @Override
    public double getMinimumInPeriod() {
        return statistics.getMin();
    }

    @Override
    public double getMaximumInPeriod() {
        return statistics.getMax();
    }

    public int getSeriesId() {
        return seriesId;
    }

    @Override
    public String toString() {
        return "{minimumValue: " + minimumValue +
                ", minimumTime: " + minimumTime +
                ", maximumValue: " + maximumValue +
                ", maximumTime: " + maximumTime +
                ", average: " + average +
                ", sum: " + getSum() +
                ", count: " + getCount() +
                ", delta: " + delta +
                ", integral: " + integral +
                ", startValue: " + startValue +
                ", firstValue: " + firstValue +
                ", firstTime: " + firstTime +
                ", lastValue: " + lastValue +
                ", lastTime: " + lastTime +
                ", periodStartTime: " + periodStart
                + ", periodEndTime: " + periodEnd + "}";
    }
}
