/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.dataImage;

import java.util.Objects;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.infiniteautomation.mango.util.Functions;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.dataImage.types.AlphanumericValue;
import com.serotonin.m2m2.rt.dataImage.types.BinaryValue;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.rt.dataImage.types.MultistateValue;
import com.serotonin.m2m2.rt.dataImage.types.NumericValue;
import com.serotonin.m2m2.view.stats.IValueTime;

/**
 * The simple value of a point at a given time.
 * 
 * @see AnnotatedPointValueTime
 * @author Matthew Lohbihler
 */
public class PointValueTime implements IValueTime<DataValue> {

    public static boolean equalValues(PointValueTime pvt1, PointValueTime pvt2) {
        if (pvt1 == null && pvt2 == null)
            return true;
        if (pvt1 == null || pvt2 == null)
            return false;
        return Objects.equals(pvt1.getValue(), pvt2.getValue());
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
    public String toString() {
        return "PointValueTime(" + value + "@" + Functions.getFullMilliSecondTime(time) + ")";
    }

    public PointValueTime withAnnotationFromSource(@Nullable SetPointSource source) {
        if (source != null && source.getSetPointSourceMessage() != null) {
            return withAnnotation(source.getSetPointSourceMessage());
        }
        return this;
    }

    public PointValueTime withAnnotation(TranslatableMessage message) {
        return new AnnotatedPointValueTime(this, message);
    }

    public IdPointValueTime withSeriesId(int seriesId) {
        return new IdPointValueTime(seriesId, value, time);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PointValueTime)) return false;
        PointValueTime that = (PointValueTime) o;
        return time == that.time && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, time);
    }
}
