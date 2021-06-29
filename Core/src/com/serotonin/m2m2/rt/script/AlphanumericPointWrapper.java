/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.script;

import java.util.List;

import javax.script.ScriptEngine;

import com.infiniteautomation.mango.statistics.ValueChangeCounter;
import com.serotonin.m2m2.rt.dataImage.IDataPointValueSource;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.util.DateUtils;

/**
 * @author Matthew Lohbihler
 */
public class AlphanumericPointWrapper extends AbstractPointWrapper {
	
    public AlphanumericPointWrapper(IDataPointValueSource point, ScriptEngine engine, ScriptPointValueSetter setter) {
        super(point, engine, setter);
    }

    public String getValue() { //always use cache here, last() for no cache
        DataValue value = getValueImpl();
        if (value == null)
            return null;
        return value.getStringValue();
    }

    public String ago(int periodType) {
        return ago(periodType, 1, false);
    }
    
    public String ago(int periodType, boolean cache) {
        return ago(periodType, 1, cache);
    }
    
    public String ago(int periodType, int count) {
        return ago(periodType, count, false);
    }

    public String ago(int periodType, int count, boolean cache) {
        long from = DateUtils.minus(getContext().getComputeTime(), periodType, count);
        PointValueTime pvt;
        if(cache || historical)
            pvt = point.getPointValueBefore(from);
        else
            pvt = valueFacade.getPointValueBefore(from);
        
        if (pvt == null)
            return null;
        return pvt.getValue().getStringValue();
    }

    public ValueChangeCounterWrapper past(int periodType) {
        return past(periodType, 1, false);
    }
    
    public ValueChangeCounterWrapper past(int periodType, boolean cache) {
        return past(periodType, 1, cache);
    }
    
    public ValueChangeCounterWrapper past(int periodType, int count) {
        return past(periodType, count, false);
    }

    public ValueChangeCounterWrapper past(int periodType, int count, boolean cache) {
        long to = getContext().getComputeTime();
        long from = DateUtils.minus(to, periodType, count);
        return getStats(from, to, cache);
    }

    public ValueChangeCounterWrapper prev(int periodType) {
        return previous(periodType, 1, false);
    }
    
    public ValueChangeCounterWrapper prev(int periodType, boolean cache) {
        return previous(periodType, 1, cache);
    }

    public ValueChangeCounterWrapper prev(int periodType, int count) {
        return previous(periodType, count, false);
    }
    
    public ValueChangeCounterWrapper prev(int periodType, int count, boolean cache) {
        return previous(periodType, count, cache);
    }

    public ValueChangeCounterWrapper previous(int periodType) {
        return previous(periodType, 1, false);
    }
    
    public ValueChangeCounterWrapper previous(int periodType, boolean cache) {
        return previous(periodType, 1, cache);
    }
    
    public ValueChangeCounterWrapper previous(int periodType, int count) {
        return previous(periodType, count, false);
    }

    public ValueChangeCounterWrapper previous(int periodType, int count, boolean cache) {
        long to = DateUtils.truncate(getContext().getComputeTime(), periodType);
        long from = DateUtils.minus(to, periodType, count);
        return getStats(from, to, cache);
    }
    
    public ValueChangeCounterWrapper getStats(long from, long to) {
        return getStats(from, to, false);
    }

    public ValueChangeCounterWrapper getStats(long from, long to, boolean cache) {
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
        ValueChangeCounter stats = new ValueChangeCounter(from, to, start, values);
        ValueChangeCounterWrapper wrapper = new ValueChangeCounterWrapper(stats);
        return wrapper;
    }

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.rt.script.AbstractPointWrapper#helpImpl(java.lang.StringBuilder)
	 */
	@Override
	protected void helpImpl(StringBuilder builder) {
    	builder.append("ago(periodType, cache): String,\n ");
    	builder.append("ago(periodType, periods, cache): String,\n ");
    	builder.append("prev(periodType, cache): String,\n ");
    	builder.append("prev(periodType, periods, cache): String,\n ");
    	builder.append("prev(periodType, cache): String,\n ");
    	builder.append("previous(periodType, periods, cache): String,\n ");
    	builder.append("previous(periodType, cache): String,\n ");
    	builder.append("getStats(from, to, cache): ValueChangeCounter,\n ");
	}
    
    
}
