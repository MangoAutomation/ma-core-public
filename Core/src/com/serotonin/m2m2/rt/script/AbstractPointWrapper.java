/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
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

    public void set(Object value) {
        set(value, getContext().getRuntime());
    }

    public void set(Object value, long timestamp) {
        if (setter != null)
            setter.set(point, value, timestamp);
    }
    
    //New methods exposed September 2014
    
    /**
     * Get point values between the times.  
     * Inclusive of the value at from, exclusisve of the value at to
     * @param from
     * @param to
     * @return List of PointValueTime objects or empty list
     */
    public List<PointValueTime> pointValuesBetween(long from, long to){
    	return point.getPointValuesBetween(from, to);
    }
    
    /**
     * Get point values since timestamp
     * @param since
     * @return List of PointValueTime objects or empty list
     */
    public List<PointValueTime> pointValuesSince(long since){
    	return point.getPointValues(since);
    	
    }
    
    /**
     * Get the nearest point value before the timestamp
     * @param timestamp
     * @return nearest value OR null
     */
    public PointValueTime pointValueBefore(long timestamp){
    	return point.getPointValueBefore(timestamp);
    }
    
    /**
     * Get the nearest point value after the timestamp
     * @param timestamp
     * @return nearest value OR null
     */
    public PointValueTime pointValueAfter(long timestamp){
    	return point.getPointValueAfter(timestamp);
    }    
    
    /**
     * Get the point value AT this time
     * @param timestamp
     * @return value at exactly this time OR null
     */
    public PointValueTime pointValueAt(long timestamp){
    	return point.getPointValueAt(timestamp);
    }
    
    /**
     * Append method descriptions
     * The { and } for the object will be added afterwards.
     * @param builder
     */
    protected abstract void helpImpl(StringBuilder builder);
    
    public String getHelp(){
    	return toString();
    }
    
    public String toString(){
    	StringBuilder builder = new StringBuilder();
    	builder.append("{\n");
    	builder.append("value: ").append(getValueImpl()).append(",\n ");
    	long time = getTime();
    	builder.append("time: ").append(time).append(",\n ");
    	if(time > 0){
	    	builder.append("millis: ").append(getMillis()).append(",\n ");
	    	builder.append("second: ").append(getSecond()).append(",\n ");
	    	builder.append("minute: ").append(getMinute()).append(",\n ");
	    	builder.append("hour: ").append(getHour()).append(",\n ");
	    	builder.append("day: ").append(getDay()).append(",\n ");
	    	builder.append("dayOfWeek: ").append(getDayOfWeek()).append(",\n ");
	    	builder.append("dayOfYear: ").append(getDayOfYear()).append(",\n ");
	    	builder.append("month: ").append(getMonth()).append(",\n ");
	    	builder.append("year: ").append(getYear()).append(",\n ");
    	}
    	builder.append("last(count): ").append("PointValueTime[count]").append(",\n ");
    	builder.append("lastValue: ").append(lastValue()).append(",\n ");
    	builder.append("lastValue(count): ").append("PointValueTime").append(",\n ");
    	builder.append("set(value): ").append(",\n ");
    	builder.append("set(value, timestamp): ").append(",\n ");
    	builder.append("pointValuesBetween(timestamp, timestamp): PointValueTime[],\n ");
    	builder.append("pointValuesSince(timestamp): PointValueTime[],\n ");
    	builder.append("pointValuesBefore(timestamp): PointValueTime[],\n ");
    	builder.append("pointValuesAfter(timestamp): PointValueTime[],\n ");
    	builder.append("pointValueAt(timestamp): PointValueTime,\n ");

    	this.helpImpl(builder);
    	
    	builder.append(" }\n");
    	return builder.toString();
    }
}
