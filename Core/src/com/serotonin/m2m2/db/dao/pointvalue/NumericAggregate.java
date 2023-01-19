/*
 * Copyright (C) 2022 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao.pointvalue;

import java.util.DoubleSummaryStatistics;
import java.util.Objects;

import com.serotonin.m2m2.rt.dataImage.types.DataValue;

/**
 * @author Jared Wiltshire
 */
public interface NumericAggregate extends AggregateValue {

    /**
     * @return minimum value. If {@link #getCount() count} is 0 will return the {@link #getStartValue() start value}
     * (if start value is null will return {@link Double#NaN NaN}).
     */
    Double getMinimumValue();

    /**
     * @return time at which {@link #getMinimumValue() min value} occurred. If {@link #getCount() count} is 0 will return the {@link #getStartValue() start value}
     * (if start value is null will return null).
     */
    Long getMinimumTime();

    /**
     * @return maximum value. If {@link #getCount() count} is 0 will return the {@link #getStartValue() start value}
     * (if start value is null will return {@link Double#NaN NaN}).
     */
    Double getMaximumValue();

    /**
     * @return time at which {@link #getMaximumValue() max value} occurred. If {@link #getCount() count} is 0 will return the {@link #getStartValue() start value}
     * (if start value is null will return null).
     */
    Long getMaximumTime();

    /**
     * @return time weighted average, if {@link #getCount() count} is 0 will return the {@link #getStartValue() start value},
     * if start value is null will return {@link Double#NaN NaN}.
     */
    Double getAverage();

    Double getIntegral();

    double getSum();

    default double getDelta() {
        double delta = Double.NaN;
        DataValue firstValue = getFirstValue();
        DataValue startValue = getStartValue();
        if (firstValue != null) {
            DataValue lastValue = getLastValue();
            delta = lastValue.getDoubleValue() - Objects.requireNonNullElse(startValue, firstValue).getDoubleValue();
        } else if (startValue != null) {
            delta = 0.0D; //No data but a start value
        }
        return delta;
    }

    default Double getAccumulator () {
        var lastValue = getLastValue();
        return lastValue == null ? getMaximumValue() : lastValue.getDoubleValue();
    }

    /**
     * @return the mean of the values in the period, if {@link #getCount() count} is 0 will return {@link Double#NaN NaN}.
     */
    default double getArithmeticMean() {
        var count = getCount();
        return count > 0 ? getSum() / count : Double.NaN;
    }

    /**
     * @return the minimum of the values in the period, if {@link #getCount() count} is 0 will return {@link Double#NaN NaN}.
     */
    double getMinimumInPeriod();

    /**
     * @return the maximum of the values in the period, if {@link #getCount() count} is 0 will return {@link Double#NaN NaN}.
     */
    double getMaximumInPeriod();

    /**
     * @return the range of values in the period, if {@link #getCount() count} is 0 will return {@link Double#NaN NaN}.
     */
    default double getRangeInPeriod() {
        double range = Double.NaN;
        double maxValue = getMaximumInPeriod();
        if (!Double.isNaN(maxValue)) {
            range = maxValue - getMinimumInPeriod();
        }
        return range;
    }

    DoubleSummaryStatistics getStatistics();
}
