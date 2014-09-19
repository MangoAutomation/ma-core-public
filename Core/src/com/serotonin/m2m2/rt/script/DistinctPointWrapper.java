/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.script;

import java.util.List;

import javax.script.ScriptEngine;

import com.serotonin.m2m2.rt.dataImage.IDataPointValueSource;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.util.DateUtils;
import com.serotonin.m2m2.view.stats.StartsAndRuntimeList;

/**
 * @author Matthew Lohbihler
 */
abstract public class DistinctPointWrapper extends AbstractPointWrapper {
    public DistinctPointWrapper(IDataPointValueSource point, ScriptEngine engine, PointValueSetter setter) {
        super(point, engine, setter);
    }

    public StartsAndRuntimeList past(int periodType) {
        return past(periodType, 1);
    }

    public StartsAndRuntimeList past(int periodType, int count) {
        long to = getContext().getRuntime();
        long from = DateUtils.minus(to, periodType, count);
        return getStats(from, to);
    }

    public StartsAndRuntimeList prev(int periodType) {
        return previous(periodType, 1);
    }

    public StartsAndRuntimeList prev(int periodType, int count) {
        return previous(periodType, count);
    }

    public StartsAndRuntimeList previous(int periodType) {
        return previous(periodType, 1);
    }

    public StartsAndRuntimeList previous(int periodType, int count) {
        long to = DateUtils.truncate(getContext().getRuntime(), periodType);
        long from = DateUtils.minus(to, periodType, count);
        return getStats(from, to);
    }

    public StartsAndRuntimeList getStats(long from, long to) {
        PointValueTime start = point.getPointValueBefore(from);
        List<PointValueTime> values = point.getPointValuesBetween(from, to);
        PointValueTime end = point.getPointValueAfter(from);
        StartsAndRuntimeList stats = new StartsAndRuntimeList(from, to, start, values, end);
        return stats;
    }
}
