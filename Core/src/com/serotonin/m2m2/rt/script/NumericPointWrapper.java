/*
 * Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
 * 
 * @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.script;

import java.util.List;

import javax.script.ScriptEngine;

import com.infiniteautomation.mango.statistics.AnalogStatistics;
import com.serotonin.m2m2.rt.dataImage.IDataPointValueSource;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.util.DateUtils;

/**
 * @author Matthew Lohbihler
 */
public class NumericPointWrapper extends AbstractPointWrapper {
    public NumericPointWrapper(IDataPointValueSource point, ScriptEngine engine,
            ScriptPointValueSetter setter) {
        super(point, engine, setter);
    }

    public Double getValue() {
        DataValue value = getValueImpl();
        if (value == null)
            return null;
        return value.getDoubleValue();
    }

    public Double ago(int periodType) {
        return ago(periodType, 1);
    }

    public Double ago(int periodType, int count) {
        long from = DateUtils.minus(getContext().getRuntime(), periodType, count);
        PointValueTime pvt = point.getPointValueBefore(from);
        if (pvt == null)
            return null;
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
        PointValueTime start = point.getPointValueBefore(from + 1);
        List<PointValueTime> values = point.getPointValuesBetween(from + 1, to);
        if(start != null && start.getTime() == from)
            values.add(0, start);
        AnalogStatistics stats = new AnalogStatistics(from, to, start, values);
        AnalogStatisticsWrapper wrapper = new AnalogStatisticsWrapper(stats);
        return wrapper;
    }

    @Override
    protected void helpImpl(StringBuilder builder) {
        builder.append("ago(periodType): double,\n ");
        builder.append("ago(periodType, periods): double,\n ");
        builder.append("past(periodType): AnalogStatisticsWrapper,\n ");
        builder.append("past(periodType, periods): AnalogStatisticsWrapper,\n ");
        builder.append("prev(periodType): AnalogStatisticsWrapper,\n ");
        builder.append("prev(periodType, periods): AnalogStatisticsWrapper,\n ");
        builder.append("previous(periodType): AnalogStatisticsWrapper,\n ");
        builder.append("previous(periodType, periods): AnalogStatisticsWrapper,\n ");
        builder.append("stats(from, to): AnalogStatisticsWrapper,\n ");
    }

}
