/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.script;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import javax.script.ScriptEngine;

import com.infiniteautomation.mango.spring.service.MangoJavaScriptService;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.rt.dataImage.HistoricalDataPoint;
import com.serotonin.m2m2.rt.dataImage.IDataPointValueSource;
import com.serotonin.m2m2.rt.dataImage.PointValueFacade;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;

/**
 * @author Matthew Lohbihler
 */
abstract public class AbstractPointWrapper {

    protected final IDataPointValueSource point;
    protected final PointValueFacade valueFacade;
    protected final ScriptEngine engine;
    protected final ScriptPointValueSetter setter;
    protected final boolean historical;
    private DataPointWrapper voWrapper = null;

    AbstractPointWrapper(IDataPointValueSource point, ScriptEngine engine, ScriptPointValueSetter setter) {
        this.point = point;
        this.valueFacade = new PointValueFacade(Common.runtimeManager.getDataPoint(point.getVO().getId()), false);
        this.engine = engine;
        this.setter = setter;
        this.historical = point instanceof HistoricalDataPoint;
    }

    protected WrapperContext getContext() {
        return (WrapperContext) engine.get(MangoJavaScriptService.WRAPPER_CONTEXT_KEY);
    }

    public DataValue getValueImpl() { //Doesn't need no-cache complement as that's the last value
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
        return last(limit, false);
    }

    public List<PointValueTime> last(int limit, boolean cache) {
        if(cache || historical)
            return point.getLatestPointValues(limit);
        else
            return valueFacade.getLatestPointValues(limit);
    }

    public PointValueTime lastValue() {
        return lastValue(0, false);
    }

    public PointValueTime lastValue(boolean cache) {
        return lastValue(0, cache);
    }

    public PointValueTime lastValue(int index) {
        return lastValue(index, false);
    }

    public PointValueTime lastValue(int index, boolean cache) {
        if(cache || historical) {
            List<PointValueTime> list = point.getLatestPointValues(index + 1);
            if (list.size() <= index)
                return null;
            return list.get(index);
        } else {
            List<PointValueTime> list = valueFacade.getLatestPointValues(index + 1);
            if (list.size() <= index)
                return null;
            return list.get(index);
        }
    }

    private Integer getCalendar(int calendarAttribute) {
        Long time = getTime();
        if (time == null)
            return null;
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTimeInMillis(time);
        return gc.get(calendarAttribute);
    }

    public void set(Object value) throws ScriptPermissionsException {
        set(value, getContext().getComputeTime());
    }

    public void set(Object value, long timestamp) throws ScriptPermissionsException {
        set(value, timestamp, null);
    }

    public void set(Object value, long timestamp, String annotation) throws ScriptPermissionsException {
        if (setter != null)
            setter.set(point, value, timestamp, annotation);
    }

    //New methods exposed September 2014

    /**
     * Get point values between the times.
     * Inclusive of the value at from, exclusive of the value at to, not using the cache
     * @return List of PointValueTime objects or empty list
     */
    public List<PointValueTime> pointValuesBetween(long from, long to){
        return pointValuesBetween(from, to, false);
    }

    /**
     * Get point values between the times.
     * Inclusive of the value at from, exclusive of the value at to, optionally using the cache
     * @return List of PointValueTime objects or empty list
     */
    public List<PointValueTime> pointValuesBetween(long from, long to, boolean cache){
        if(cache || historical)
            return point.getPointValuesBetween(from, to);
        else
            return valueFacade.getPointValuesBetween(from, to);
    }

    /**
     * Get point values since timestamp, not using cache
     * @return List of PointValueTime objects or empty list
     */
    public List<PointValueTime> pointValuesSince(long since){
        return pointValuesSince(since, false);

    }

    /**
     * Get point values since timestamp, optionally using cache
     * @return List of PointValueTime objects or empty list
     */
    public List<PointValueTime> pointValuesSince(long since, boolean cache){
        if(cache || historical)
            return point.getPointValues(since);
        else
            return valueFacade.getPointValues(since);
    }

    /**
     * Get the nearest point value before the timestamp, not using cache
     * @return nearest value OR null
     */
    public PointValueTime pointValueBefore(long timestamp){
        return pointValueBefore(timestamp, false);
    }

    /**
     * Get the nearest point value before the timestamp, optionally using cache
     * @return nearest value OR null
     */
    public PointValueTime pointValueBefore(long timestamp, boolean cache){
        if(cache || historical)
            return point.getPointValueBefore(timestamp);
        else
            return valueFacade.getPointValueBefore(timestamp);
    }

    /**
     * Get the nearest point value after the timestamp, not using cache
     * @return nearest value OR null
     */
    public PointValueTime pointValueAfter(long timestamp){
        return pointValueAfter(timestamp, false);
    }

    /**
     * Get the nearest point value after the timestamp, optionally using cache
     * @return nearest value OR null
     */
    public PointValueTime pointValueAfter(long timestamp, boolean cache){
        if(cache || historical)
            return point.getPointValueAfter(timestamp);
        else
            return valueFacade.getPointValueAfter(timestamp);
    }

    /**
     * Get the point value AT this time, not using cache
     * @return value at exactly this time OR null
     */
    public PointValueTime pointValueAt(long timestamp){
        return pointValueAt(timestamp, false);
    }

    /**
     * Get the point value AT this time, optionally using cache
     * @return value at exactly this time OR null
     */
    public PointValueTime pointValueAt(long timestamp, boolean cache){
        if(cache || historical)
            return point.getPointValueAt(timestamp);
        else
            return valueFacade.getPointValueAt(timestamp);
    }

    /**
     * Get the wrapper for the data point's vo
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
     */
    protected abstract void helpImpl(StringBuilder builder);

    public String getHelp(){
        return toString();
    }

    @Override
    public String toString(){
        StringBuilder builder = new StringBuilder();
        builder.append("/* cache argument is true / false and false if omitted */");
        builder.append("{\n");
        builder.append("value: ").append(getValueImpl()).append(",\n ");
        Long time = getTime();
        builder.append("time: ").append(time).append(",\n ");
        if(time != null){
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
        builder.append("last(count, cache): ").append("PointValueTime[count]").append(",\n ");
        builder.append("lastValue(cache): ").append(lastValue()).append(",\n ");
        builder.append("lastValue(count, cache): ").append("PointValueTime").append(",\n ");
        builder.append("set(value): ").append(",\n ");
        builder.append("set(value, timestamp): ").append(",\n ");
        builder.append("set(value, timestamp, annotation): ").append(",\n ");
        builder.append("pointValuesBetween(timestamp, timestamp, cache): PointValueTime[],\n ");
        builder.append("pointValuesSince(timestamp, cache): PointValueTime[],\n ");
        builder.append("pointValuesBefore(timestamp, cache): PointValueTime[],\n ");
        builder.append("pointValuesAfter(timestamp, cache): PointValueTime[],\n ");
        builder.append("pointValueAt(timestamp, cache): PointValueTime,\n ");
        builder.append("getDataPointWrapper(): DataPointWrapper,\n ");

        this.helpImpl(builder);

        builder.append(" }\n");
        return builder.toString();
    }
}
