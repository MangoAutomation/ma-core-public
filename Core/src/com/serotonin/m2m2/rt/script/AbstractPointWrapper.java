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
    protected final ScriptPointValueSetter setter;
    private DataPointWrapper voWrapper = null;

    AbstractPointWrapper(IDataPointValueSource point, ScriptEngine engine, ScriptPointValueSetter setter) {
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

    public Long getTime() {
        PointValueTime pvt = point.getPointValue();
        if (pvt == null)
            return null;
        return pvt.getTime();
    }

    public Integer getMillis() {
        return getCalendar(Calendar.MILLISECOND);
    }

    public Integer getSecond() {
        return getCalendar(Calendar.SECOND);
    }

    public Integer getMinute() {
        return getCalendar(Calendar.MINUTE);
    }

    public Integer getHour() {
        return getCalendar(Calendar.HOUR_OF_DAY);
    }

    public Integer getDay() {
        return getCalendar(Calendar.DATE);
    }

    public Integer getDayOfWeek() {
        return getCalendar(Calendar.DAY_OF_WEEK);
    }

    public Integer getDayOfYear() {
        return getCalendar(Calendar.DAY_OF_YEAR);
    }

    public Integer getMonth() {
        Integer month = getCalendar(Calendar.MONTH);
        if(month != null)
            return month + 1;
        return null;
    }

    public Integer getYear() {
        return getCalendar(Calendar.YEAR);
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

    private Integer getCalendar(int calendarAttribute) {
        Long time = getTime();
        if (time == null)
            return null;
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTimeInMillis(time);
        return gc.get(calendarAttribute);
    }

    public void set(Object value) {
        set(value, getContext().getRuntime());
    }
    
    public void set(Object value, long timestamp) {
    	set(value, timestamp, null);
    }

    public void set(Object value, long timestamp, String annotation) {
        if (setter != null)
            setter.set(point, value, timestamp, annotation);
    }
    
    //New methods exposed September 2014
    
    /**
     * Get point values between the times.  
     * Inclusive of the value at from, exclusive of the value at to
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
     * Get the wrapper for the data point's vo
     * @param
     * @return vo of wrapper data point, as DataPointWrapper
     */
    public DataPointWrapper getDataPointWrapper(){
    	if(voWrapper == null)
    		voWrapper = point.getDataPointWrapper(this);
    	return voWrapper;
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
    	builder.append("set(value, timestamp, annotation): ").append(",\n ");
    	builder.append("pointValuesBetween(timestamp, timestamp): PointValueTime[],\n ");
    	builder.append("pointValuesSince(timestamp): PointValueTime[],\n ");
    	builder.append("pointValuesBefore(timestamp): PointValueTime[],\n ");
    	builder.append("pointValuesAfter(timestamp): PointValueTime[],\n ");
    	builder.append("pointValueAt(timestamp): PointValueTime,\n ");
    	builder.append("pointValueAt(timestamp): PointValueTime,\n ");
    	builder.append("getDataPointWrapper(): DataPointWrapper,\n ");

    	this.helpImpl(builder);
    	
    	builder.append(" }\n");
    	return builder.toString();
    }
}
