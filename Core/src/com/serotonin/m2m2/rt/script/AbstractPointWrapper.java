/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.script;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import javax.script.ScriptEngine;

import com.serotonin.m2m2.rt.dataImage.IDataPointValueSource;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;

/**
 * @author Matthew Lohbihler
 */
abstract public class AbstractPointWrapper {
    protected final IDataPointValueSource point;
    protected final ScriptEngine engine;
    protected final PointValueSetter setter;

    AbstractPointWrapper(IDataPointValueSource point, ScriptEngine engine, PointValueSetter setter) {
        this.point = point;
        this.engine = engine;
        this.setter = setter;
    }

    protected WrapperContext getContext() {
        return (WrapperContext) engine.get(ScriptUtils.WRAPPER_CONTEXT_KEY);
    }

    protected DataValue getValueImpl() {
        PointValueTime pvt = point.getPointValue();
        if (pvt == null)
            return null;
        return pvt.getValue();
    }

    public long getTime() {
        PointValueTime pvt = point.getPointValue();
        if (pvt == null)
            return -1;
        return pvt.getTime();
    }

    public int getMillis() {
        return getCalendar().get(Calendar.MILLISECOND);
    }

    public int getSecond() {
        return getCalendar().get(Calendar.SECOND);
    }

    public int getMinute() {
        return getCalendar().get(Calendar.MINUTE);
    }

    public int getHour() {
        return getCalendar().get(Calendar.HOUR_OF_DAY);
    }

    public int getDay() {
        return getCalendar().get(Calendar.DATE);
    }

    public int getDayOfWeek() {
        return getCalendar().get(Calendar.DAY_OF_WEEK);
    }

    public int getDayOfYear() {
        return getCalendar().get(Calendar.DAY_OF_YEAR);
    }

    public int getMonth() {
        return getCalendar().get(Calendar.MONTH) + 1;
    }

    public int getYear() {
        return getCalendar().get(Calendar.YEAR);
    }

    public List<PointValueTime> last(int limit) {
        return point.getLatestPointValues(limit);
    }

    public PointValueTime lastValue() {
        return lastValue(0);
    }

    public PointValueTime lastValue(int index) {
        List<PointValueTime> list = point.getLatestPointValues(index + 1);
        if (list.size() <= index)
            return null;
        return list.get(index);
    }

    private GregorianCalendar getCalendar() {
        long time = getTime();
        if (time == -1)
            return null;
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTimeInMillis(time);
        return gc;
    }

    public String getHelp() {
        return toString();
    }

    public void set(Object value) {
        set(value, getContext().getRuntime());
    }

    public void set(Object value, long timestamp) {
        if (setter != null)
            setter.set(point, value, timestamp);
    }
}
