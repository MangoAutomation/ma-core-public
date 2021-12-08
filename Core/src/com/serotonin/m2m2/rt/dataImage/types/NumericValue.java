/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.dataImage.types;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataType;

/**
 * @author Matthew Lohbihler
 */
public class NumericValue extends DataValue implements Comparable<NumericValue> {
    
    public static NumericValue parseNumeric(String s) {
        return parseNumeric(s, Common.getLocale());
    }
    
    public static NumericValue parseNumeric(String s, Locale locale){
        if (s == null)
            return new NumericValue(0);
        try {
        	NumberFormat format = NumberFormat.getInstance(locale);
            Number number = format.parse(s);
            return new NumericValue(number.doubleValue());
        }
        catch (ParseException e) {
            // no op
        }
        return new NumericValue(0);
    }

    private final double value;

    public NumericValue(double value) {
        this.value = value;
    }

    @Override
    public boolean hasDoubleRepresentation() {
        return true;
    }

    @Override
    public double getDoubleValue() {
        return value;
    }

    public float getFloatValue() {
        return (float) value;
    }

    @Override
    public String getStringValue() {
        return null;
    }

    @Override
    public boolean getBooleanValue() {
        throw new RuntimeException("NumericValue has no boolean value.");
    }

    @Override
    public Object getObjectValue() {
        return value;
    }

    @Override
    public int getIntegerValue() {
        return (int) value;
    }

    @Override
    public Number numberValue() {
        return value;
    }

    @Override
    public DataType getDataType() {
        return DataType.NUMERIC;
    }

    @Override
    public String toString() {
        return Double.toString(value);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(value);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final NumericValue other = (NumericValue) obj;
        if (Double.doubleToLongBits(value) != Double.doubleToLongBits(other.value))
            return false;
        return true;
    }

    @Override
    public int compareTo(NumericValue that) {
        return Double.compare(value, that.value);
    }

    @Override
    public <T extends DataValue> int compareTo(T that) {
        return compareTo((NumericValue) that);
    }
}
