/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
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
                + "previous(periodType, count), last(limit), lastValue(index)}";
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

    public AnalogStatistics past(int periodType) {
        return past(periodType, 1);
    }

    public AnalogStatistics past(int periodType, int count) {
        long to = getContext().getRuntime();
        long from = DateUtils.minus(to, periodType, count);
        return getStats(from, to);
    }

    public AnalogStatistics prev(int periodType) {
        return previous(periodType, 1);
    }

    public AnalogStatistics prev(int periodType, int count) {
        return previous(periodType, count);
    }

    public AnalogStatistics previous(int periodType) {
        return previous(periodType, 1);
    }

    public AnalogStatistics previous(int periodType, int count) {
        long to = DateUtils.truncate(getContext().getRuntime(), periodType);
        long from = DateUtils.minus(to, periodType, count);
        return getStats(from, to);
    }

    private AnalogStatistics getStats(long from, long to) {
        PointValueTime start = point.getPointValueBefore(from);
        List<PointValueTime> values = point.getPointValuesBetween(from, to);
        PointValueTime end = point.getPointValueAfter(from);
        AnalogStatistics stats = new AnalogStatistics(from, to, start, values, end);
        return stats;
    }
}
