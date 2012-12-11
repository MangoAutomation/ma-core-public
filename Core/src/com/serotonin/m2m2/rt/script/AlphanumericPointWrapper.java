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

    @Override
    public String toString() {
        return "{value=" + getValue() + ", ago(periodType, count), last(limit), lastValue(index)}";
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

    public ValueChangeCounter past(int periodType) {
        return past(periodType, 1);
    }

    public ValueChangeCounter past(int periodType, int count) {
        long to = getContext().getRuntime();
        long from = DateUtils.minus(to, periodType, count);
        return getStats(from, to);
    }

    public ValueChangeCounter prev(int periodType) {
        return previous(periodType, 1);
    }

    public ValueChangeCounter prev(int periodType, int count) {
        return previous(periodType, count);
    }

    public ValueChangeCounter previous(int periodType) {
        return previous(periodType, 1);
    }

    public ValueChangeCounter previous(int periodType, int count) {
        long to = DateUtils.truncate(getContext().getRuntime(), periodType);
        long from = DateUtils.minus(to, periodType, count);
        return getStats(from, to);
    }

    private ValueChangeCounter getStats(long from, long to) {
        PointValueTime start = point.getPointValueBefore(from);
        List<PointValueTime> values = point.getPointValuesBetween(from, to);
        ValueChangeCounter stats = new ValueChangeCounter(from, to, start, values);
        return stats;
    }
}
