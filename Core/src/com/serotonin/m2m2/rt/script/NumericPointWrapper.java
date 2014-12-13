/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.script;

import java.util.List;

import javax.script.ScriptEngine;

import com.serotonin.m2m2.rt.dataImage.IDataPointValueSource;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.util.DateUtils;
import com.serotonin.m2m2.view.stats.AnalogStatistics;

/**
 * @author Matthew Lohbihler
 */
public class NumericPointWrapper extends AbstractPointWrapper {
    public NumericPointWrapper(IDataPointValueSource point, ScriptEngine engine, PointValueSetter setter) {
        super(point, engine, setter);
    }

    public double getValue() {
        DataValue value = getValueImpl();
        if (value == null)
            return 0;
        return value.getDoubleValue();
    }

    @Override
    public String toString() {
        return "{value=" + getValue() + ", ago(periodType, count), past(periodType, count), prev(periodType, count), "
                + "previous(periodType, count), last(limit), lastValue(index), getStats(from, to)}";
    }

    public double ago(int periodType) {
        return ago(periodType, 1);
    }

    public double ago(int periodType, int count) {
        long from = DateUtils.minus(getContext().getRuntime(), periodType, count);
        PointValueTime pvt = point.getPointValueBefore(from);
        if (pvt == null)
            return 0;
        return pvt.getDoubleValue();
    }

    public AnalogStatisticsWrapper past(int periodType) {
        return past(periodType, 1);
    }

    public AnalogStatisticsWrapper past(int periodType, int count) {
        long to = getContext().getRuntime();
        long from = DateUtils.minus(to, periodType, count);
        return getStats(from, to);
    }

    public AnalogStatisticsWrapper prev(int periodType) {
        return previous(periodType, 1);
    }

    public AnalogStatisticsWrapper prev(int periodType, int count) {
        return previous(periodType, count);
    }

    public AnalogStatisticsWrapper previous(int periodType) {
        return previous(periodType, 1);
    }

    public AnalogStatisticsWrapper previous(int periodType, int count) {
        long to = DateUtils.truncate(getContext().getRuntime(), periodType);
        long from = DateUtils.minus(to, periodType, count);
        return getStats(from, to);
    }

    public AnalogStatisticsWrapper getStats(long from, long to) {
        PointValueTime start = point.getPointValueBefore(from);
        List<PointValueTime> values = point.getPointValuesBetween(from, to);
        PointValueTime end = point.getPointValueAfter(to);
        AnalogStatistics stats = new AnalogStatistics(from, to, start, values, end);
        AnalogStatisticsWrapper wrapper = new AnalogStatisticsWrapper(stats);
        return wrapper;
    }
}
