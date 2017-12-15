/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.script;

import java.util.List;

import javax.script.ScriptEngine;

import com.infiniteautomation.mango.statistics.StartsAndRuntimeList;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.rt.dataImage.IDataPointValueSource;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.util.DateUtils;

/**
 * @author Matthew Lohbihler
 */
abstract public class DistinctPointWrapper extends AbstractPointWrapper {
    public DistinctPointWrapper(IDataPointValueSource point, ScriptEngine engine, ScriptPointValueSetter setter) {
        super(point, engine, setter);
    }

    public StartsAndRuntimeListWrapper past(int periodType) {
        return past(periodType, 1);
    }

    public StartsAndRuntimeListWrapper past(int periodType, int count) {
        long to = getContext().getRuntime();
        long from = DateUtils.minus(to, periodType, count);
        return getStats(from, to);
    }

    public StartsAndRuntimeListWrapper prev(int periodType) {
        return previous(periodType, 1);
    }

    public StartsAndRuntimeListWrapper prev(int periodType, int count) {
        return previous(periodType, count);
    }

    public StartsAndRuntimeListWrapper previous(int periodType) {
        return previous(periodType, 1);
    }

    public StartsAndRuntimeListWrapper previous(int periodType, int count) {
        long to = DateUtils.truncate(getContext().getRuntime(), periodType);
        long from = DateUtils.minus(to, periodType, count);
        return getStats(from, to);
    }

    public StartsAndRuntimeListWrapper getStats(long from, long to) {
        PointValueTime start = point.getPointValueBefore(from);
        List<PointValueTime> values = point.getPointValuesBetween(from, to);
        StartsAndRuntimeList stats = new StartsAndRuntimeList(from, to, start, values);
        if (point.getDataTypeId() == DataTypes.BINARY)
            return new BinaryStartsAndRuntimeListWrapper(stats);
        else
            return new MultistateStartsAndRuntimeListWrapper(stats);
    }
    
    @Override
	protected void helpImpl(StringBuilder builder) {
    	builder.append("past(periodType): StartsAndRuntimeList,\n ");	
    	builder.append("past(periodType, periods): StartsAndRuntimeList,\n ");	
    	builder.append("prev(periodType): StartsAndRuntimeList,\n ");	
    	builder.append("prev(periodType, periods): StartsAndRuntimeList,\n ");	
    	builder.append("previous(periodType): StartsAndRuntimeList,\n ");	
    	builder.append("previous(periodType, periods): StartsAndRuntimeList,\n ");	
    	builder.append("stats(from, to): StartsAndRuntimeList,\n ");	
    }
}
