/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.script;

import java.util.List;

import javax.script.ScriptEngine;

import com.infiniteautomation.mango.statistics.StartsAndRuntimeList;
import com.serotonin.m2m2.DataType;
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
        return past(periodType, 1, false);
    }

    public StartsAndRuntimeListWrapper past(int periodType, boolean cache) {
        return past(periodType, 1, cache);
    }
    
    public StartsAndRuntimeListWrapper past(int periodType, int count) {
        return past(periodType, count, false);
    }
    
    public StartsAndRuntimeListWrapper past(int periodType, int count, boolean cache) {
        long to = getContext().getComputeTime();
        long from = DateUtils.minus(to, periodType, count);
        return getStats(from, to, cache);
    }

    public StartsAndRuntimeListWrapper prev(int periodType) {
        return previous(periodType, 1, false);
    }
    
    public StartsAndRuntimeListWrapper prev(int periodType, boolean cache) {
        return previous(periodType, 1, cache);
    }

    public StartsAndRuntimeListWrapper prev(int periodType, int count) {
        return previous(periodType, count, false);
    }
    
    public StartsAndRuntimeListWrapper prev(int periodType, int count, boolean cache) {
        return previous(periodType, count, cache);
    }

    public StartsAndRuntimeListWrapper previous(int periodType) {
        return previous(periodType, 1, false);
    }
    
    public StartsAndRuntimeListWrapper previous(int periodType, boolean cache) {
        return previous(periodType, 1, cache);
    }

    public StartsAndRuntimeListWrapper previous(int periodType, int count) {
        return previous(periodType, count, false);
    }
    
    public StartsAndRuntimeListWrapper previous(int periodType, int count, boolean cache) {
        long to = DateUtils.truncate(getContext().getComputeTime(), periodType);
        long from = DateUtils.minus(to, periodType, count);
        return getStats(from, to, cache);
    }
    
    public StartsAndRuntimeListWrapper getStats(long from, long to) {
        return getStats(from, to, false);
    }

    public StartsAndRuntimeListWrapper getStats(long from, long to, boolean cache) {
        PointValueTime start;
        List<PointValueTime> values;
        if(cache || historical) {
            start = point.getPointValueBefore(from + 1);
            values = point.getPointValuesBetween(from + 1, to);
        } else {
            start = valueFacade.getPointValueBefore(from+1);
            values = valueFacade.getPointValuesBetween(from + 1, to);
        }
        
        if(start != null && start.getTime() == from)
            values.add(0, start);
        StartsAndRuntimeList stats = new StartsAndRuntimeList(from, to, start, values);
        if (point.getDataType() == DataType.BINARY)
            return new BinaryStartsAndRuntimeListWrapper(stats);
        else
            return new MultistateStartsAndRuntimeListWrapper(stats);
    }
    
    @Override
	protected void helpImpl(StringBuilder builder) {
    	builder.append("past(periodType, cache): StartsAndRuntimeList,\n ");	
    	builder.append("past(periodType, periods, cache): StartsAndRuntimeList,\n ");	
    	builder.append("prev(periodType, cache): StartsAndRuntimeList,\n ");	
    	builder.append("prev(periodType, periods, cache): StartsAndRuntimeList,\n ");	
    	builder.append("previous(periodType, cache): StartsAndRuntimeList,\n ");	
    	builder.append("previous(periodType, periods, cache): StartsAndRuntimeList,\n ");	
    	builder.append("getStats(from, to, cache): StartsAndRuntimeList,\n ");	
    }
}
