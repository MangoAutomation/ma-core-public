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
import com.serotonin.m2m2.view.stats.ValueChangeCounter;

/**
 * @author Matthew Lohbihler
 */
public class AlphanumericPointWrapper extends AbstractPointWrapper {
	
    public AlphanumericPointWrapper(IDataPointValueSource point, ScriptEngine engine, PointValueSetter setter) {
        super(point, engine, setter);
    }

    public String getValue() {
        DataValue value = getValueImpl();
        if (value == null)
            return "";
        return value.getStringValue();
    }

    public String ago(int periodType) {
        return ago(periodType, 1);
    }

    public String ago(int periodType, int count) {
        long from = DateUtils.minus(getContext().getRuntime(), periodType, count);
        PointValueTime pvt = point.getPointValueBefore(from);
        if (pvt == null)
            return null;
        return pvt.getValue().getStringValue();
    }

    public ValueChangeCounterWrapper past(int periodType) {
        return past(periodType, 1);
    }

    public ValueChangeCounterWrapper past(int periodType, int count) {
        long to = getContext().getRuntime();
        long from = DateUtils.minus(to, periodType, count);
        return getStats(from, to);
    }

    public ValueChangeCounterWrapper prev(int periodType) {
        return previous(periodType, 1);
    }

    public ValueChangeCounterWrapper prev(int periodType, int count) {
        return previous(periodType, count);
    }

    public ValueChangeCounterWrapper previous(int periodType) {
        return previous(periodType, 1);
    }

    public ValueChangeCounterWrapper previous(int periodType, int count) {
        long to = DateUtils.truncate(getContext().getRuntime(), periodType);
        long from = DateUtils.minus(to, periodType, count);
        return getStats(from, to);
    }

    public ValueChangeCounterWrapper getStats(long from, long to) {
        PointValueTime start = point.getPointValueBefore(from);
        List<PointValueTime> values = point.getPointValuesBetween(from, to);
        ValueChangeCounter stats = new ValueChangeCounter(from, to, start, values);
        ValueChangeCounterWrapper wrapper = new ValueChangeCounterWrapper(stats);
        return wrapper;
    }

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.rt.script.AbstractPointWrapper#helpImpl(java.lang.StringBuilder)
	 */
	@Override
	protected void helpImpl(StringBuilder builder) {
    	builder.append("ago(periodType): String,\n ");
    	builder.append("ago(periodType, periods): String,\n ");
    	builder.append("prev(periodType): String,\n ");
    	builder.append("prev(periodType, periods): String,\n ");
    	builder.append("prev(periodType): String,\n ");
    	builder.append("previous(periodType, periods): String,\n ");
    	builder.append("previous(periodType): String,\n ");
    	builder.append("stats(from, to): ValueChangeCounter,\n ");
	}
    
    
}
