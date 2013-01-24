/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.view.stats;

import java.util.List;

import com.serotonin.m2m2.rt.dataImage.types.DataValue;

/**
 * @author Matthew Lohbihler
 */
public class AnalogStatistics implements StatisticsGenerator {
    // Configuration values.
    private final long periodStart;
    private final long periodEnd;

    // Calculated values.
    private Double minimumValue;
    private Long minimumTime;
    private Double maximumValue;
    private Long maximumTime;
    private Double average;
    private double sum;
    private Double firstValue;
    private Long firstTime;
    private Double lastValue;
    private Long lastTime;
    private int count;

    // State values. Used for calculating weighted average.
    private Double latestValue;
    private long latestTime;
    private long totalDuration;

    public AnalogStatistics(long periodStart, long periodEnd, IValueTime startVT, List<? extends IValueTime> values,
            IValueTime endVT) {
        this(periodStart, periodEnd, startVT == null ? null : startVT.getValue().getDoubleValue(), values,
                endVT == null ? null : endVT.getValue().getDoubleValue());
    }

    public AnalogStatistics(long periodStart, long periodEnd, Double startValue, List<? extends IValueTime> values,
            Double endValue) {
        this(periodStart, periodEnd, startValue);
        for (IValueTime p : values)
            addValueTime(p);
        done(endValue);
    }

    public AnalogStatistics(long periodStart, long periodEnd, Double startValue) {
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;

        if (startValue != null) {
            minimumValue = maximumValue = latestValue = startValue;
            minimumTime = maximumTime = latestTime = periodStart;
        }
    }

    @Override
    public void addValueTime(IValueTime vt) {
        addValueTime(vt.getValue(), vt.getTime());
    }

    public void addValueTime(DataValue value, long time) {
        if (value == null)
            return;

        double doubleValue = value.getDoubleValue();

        count++;

        if (firstValue == null) {
            firstValue = doubleValue;
            firstTime = time;
        }

        if (minimumValue == null || minimumValue > doubleValue) {
            minimumValue = doubleValue;
            minimumTime = time;
        }

        if (maximumValue == null || maximumValue < doubleValue) {
            maximumValue = doubleValue;
            maximumTime = time;
        }

        updateAverage(doubleValue, time);

        sum += value.getDoubleValue();

        lastValue = value.getDoubleValue();
        lastTime = time;
    }

    @Override
    public void done(IValueTime endVT) {
        done(endVT == null ? null : endVT.getValue().getDoubleValue());
    }

    public void done(Double endValue) {
        if (endValue != null)
            // There is an end value, so add the weighted latest value to the average, using the period end for
            // duration calculation.
            updateAverage(endValue, periodEnd);

        if (average != null)
            average /= totalDuration;
        else
            // Special case: if there was no start value and no end value, and only one value in the data set, we will
            // have a latest value, and a duration of zero. For this value we set the average equal to that value.
            average = lastValue;
    }

    private void updateAverage(double value, long time) {
        if (latestValue != null) {
            // The duration for which the last value was in force.
            long duration = time - latestTime;

            if (duration > 0) {
                // Determine the weighted average of the latest value. The average value at this point still needs to
                // be divided by the total duration of the period.
                if (average == null)
                    average = 0D;
                average += latestValue * duration;
                totalDuration += duration;
            }
        }

        // Reset the latest value.
        latestValue = value;
        lastTime = time;
    }

    @Override
    public long getPeriodStartTime() {
        return periodStart;
    }

    @Override
    public long getPeriodEndTime() {
        return periodEnd;
    }

    public Double getMinimumValue() {
        return minimumValue;
    }

    public Long getMinimumTime() {
        return minimumTime;
    }

    public Double getMaximumValue() {
        return maximumValue;
    }

    public Long getMaximumTime() {
        return maximumTime;
    }

    public Double getAverage() {
        return average;
    }

    public double getSum() {
        return sum;
    }

    public Double getFirstValue() {
        return firstValue;
    }

    public Long getFirstTime() {
        return firstTime;
    }

    public Double getLastValue() {
        return lastValue;
    }

    public Long getLastTime() {
        return lastTime;
    }

    public int getCount() {
        return count;
    }

    public String getHelp() {
        return toString();
    }

    @Override
    public String toString() {
        return "{minimumValue: " + minimumValue + ", minimumTime=" + minimumTime + ", maximumValue: " + maximumValue
                + ", maximumTime=" + maximumTime + ", average: " + average + ", sum: " + sum + ", count: " + count
                + ", firstValue: " + firstValue + ", firstTime: " + firstTime + ", lastValue: " + lastValue
                + ", lastTime: " + lastTime + ", periodStart: " + periodStart + ", periodEnd: " + periodEnd + "}";
    }
}
