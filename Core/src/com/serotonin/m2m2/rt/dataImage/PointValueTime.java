/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.dataImage;

import java.io.Serializable;

import org.apache.commons.lang3.ObjectUtils;

import com.serotonin.m2m2.rt.dataImage.types.AlphanumericValue;
import com.serotonin.m2m2.rt.dataImage.types.BinaryValue;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.rt.dataImage.types.MultistateValue;
import com.serotonin.m2m2.rt.dataImage.types.NumericValue;
import com.serotonin.m2m2.view.stats.IValueTime;
import com.serotonin.m2m2.web.taglib.Functions;

/**
 * The simple value of a point at a given time.
 * 
 * @see AnnotatedPointValueTime
 * @author Matthew Lohbihler
 */
public class PointValueTime implements Serializable, IValueTime, Comparable<PointValueTime> {
    private static final long serialVersionUID = -1;

    public static boolean equalValues(PointValueTime pvt1, PointValueTime pvt2) {
        if (pvt1 == null && pvt2 == null)
            return true;
        if (pvt1 == null || pvt2 == null)
            return false;
        return ObjectUtils.equals(pvt1.getValue(), pvt2.getValue());
    }

    public static DataValue getValue(PointValueTime pvt) {
        if (pvt == null)
            return null;
        return pvt.getValue();
    }

    private final DataValue value;
    private final long time;

    public PointValueTime(DataValue value, long time) {
        this.value = value;
        this.time = time;
    }

    public PointValueTime(boolean value, long time) {
        this(new BinaryValue(value), time);
    }

    public PointValueTime(int value, long time) {
        this(new MultistateValue(value), time);
    }

    public PointValueTime(double value, long time) {
        this(new NumericValue(value), time);
    }

    public PointValueTime(String value, long time) {
        this(new AlphanumericValue(value), time);
    }

    @Override
    public long getTime() {
        return time;
    }
    
    @Override
    public DataValue getValue() {
        return value;
    }

    public boolean isAnnotated() {
        return false;
    }

    public double getDoubleValue() {
        return value.getDoubleValue();
    }

    public String getStringValue() {
        return value.getStringValue();
    }

    public int getIntegerValue() {
        return value.getIntegerValue();
    }

    public boolean getBooleanValue() {
        return value.getBooleanValue();
    }

    @Override
    public boolean equals(Object o) {
        PointValueTime that = (PointValueTime) o;
        if (time != that.time)
            return false;
        return ObjectUtils.equals(value, that.value);
    }

    @Override
    public String toString() {
        return "PointValueTime(" + value + "@" + Functions.getTime(time) + ")";
    }

    @Override
    public int compareTo(PointValueTime that) {
        if (time < that.time)
            return -1;
        if (time > that.time)
            return 1;
        return 0;
    }

}
