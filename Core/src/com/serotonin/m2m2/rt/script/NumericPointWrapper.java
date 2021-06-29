/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
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

    public Double getValue() { //always use cache here, last() for no cache
        DataValue value = getValueImpl();
        if (value == null)
            return null;
        return value.getDoubleValue();
    }

    public Double ago(int periodType) {
        return ago(periodType, 1, false);
    }
    
    public Double ago(int periodType, boolean cache) {
        return ago(periodType, 1, cache);
    }
    
    public Double ago(int periodType, int count) {
        return ago(periodType, count, false);
    }

    public Double ago(int periodType, int count, boolean cache) {
        long from = DateUtils.minus(getContext().getComputeTime(), periodType, count);
        PointValueTime pvt;
        if(cache || historical)
            pvt = point.getPointValueBefore(from);
        else
            pvt = valueFacade.getPointValueBefore(from);
        if (pvt == null)
            return null;
        return pvt.getDoubleValue();
    }

    public AnalogStatisticsWrapper past(int periodType) {
        return past(periodType, 1, false);
    }
    
    public AnalogStatisticsWrapper past(int periodType, boolean cache) {
        return past(periodType, 1, cache);
    }
    
    public AnalogStatisticsWrapper past(int periodType, int count) {
        return past(periodType, count, false);
    }

    public AnalogStatisticsWrapper past(int periodType, int count, boolean cache) {
        long to = getContext().getComputeTime();
        long from = DateUtils.minus(to, periodType, count);
        return getStats(from, to, cache);
    }

    public AnalogStatisticsWrapper prev(int periodType) {
        return previous(periodType, 1, false);
    }
    
    public AnalogStatisticsWrapper prev(int periodType, boolean cache) {
        return previous(periodType, 1, cache);
    }

    public AnalogStatisticsWrapper prev(int periodType, int count) {
        return previous(periodType, count, false);
    }
    
    public AnalogStatisticsWrapper prev(int periodType, int count, boolean cache) {
        return previous(periodType, count, cache);
    }

    public AnalogStatisticsWrapper previous(int periodType) {
        return previous(periodType, 1, false);
    }
    
    public AnalogStatisticsWrapper previous(int periodType, boolean cache) {
        return previous(periodType, 1, cache);
    }
    
    public AnalogStatisticsWrapper previous(int periodType, int count) {
        return previous(periodType, count, false);
    }

    public AnalogStatisticsWrapper previous(int periodType, int count, boolean cache) {
        long to = DateUtils.truncate(getContext().getComputeTime(), periodType);
        long from = DateUtils.minus(to, periodType, count);
        return getStats(from, to, cache);
    }
    
    public AnalogStatisticsWrapper getStats(long from, long to) {
        return getStats(from, to, false);
    }

    public AnalogStatisticsWrapper getStats(long from, long to, boolean cache) {
        PointValueTime start;
        List<PointValueTime> values;
        if(cache || historical) {
            start = point.getPointValueBefore(from + 1);
            values = point.getPointValuesBetween(from + 1, to);
        } else {
            start = valueFacade.getPointValueBefore(from + 1);
            values = valueFacade.getPointValuesBetween(from + 1, to);
        }
        
        if(start != null && start.getTime() == from)
            values.add(0, start);
        AnalogStatistics stats = new AnalogStatistics(from, to, start, values);
        AnalogStatisticsWrapper wrapper = new AnalogStatisticsWrapper(stats);
        return wrapper;
    }

    @Override
    protected void helpImpl(StringBuilder builder) {
        builder.append("ago(periodType, cache): double,\n ");
        builder.append("ago(periodType, periods, cache): double,\n ");
        builder.append("past(periodType, cache): AnalogStatisticsWrapper,\n ");
        builder.append("past(periodType, periods, cache): AnalogStatisticsWrapper,\n ");
        builder.append("prev(periodType, cache): AnalogStatisticsWrapper,\n ");
        builder.append("prev(periodType, periods, cache): AnalogStatisticsWrapper,\n ");
        builder.append("previous(periodType, cache): AnalogStatisticsWrapper,\n ");
        builder.append("previous(periodType, periods, cache): AnalogStatisticsWrapper,\n ");
        builder.append("getStats(from, to, cache): AnalogStatisticsWrapper,\n ");
    }

}
