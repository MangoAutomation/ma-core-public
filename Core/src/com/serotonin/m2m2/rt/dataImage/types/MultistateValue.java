/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.dataImage.types;

import com.serotonin.m2m2.DataTypes;

/**
 * @author Matthew Lohbihler
 */
public class MultistateValue extends DataValue implements Comparable<MultistateValue> {
    public static MultistateValue parseMultistate(String s) {
        if (s == null)
            return new MultistateValue(0);
        try {
            return new MultistateValue(Integer.parseInt(s));
        }
        catch (NumberFormatException e) {
            // no op
        }
        return new MultistateValue(0);
    }

    private final int value;

    public MultistateValue(int value) {
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

    @Override
    public String getStringValue() {
        return null;
    }

    @Override
    public boolean getBooleanValue() {
        throw new RuntimeException("MultistateValue has no boolean value.");
    }

    @Override
    public Object getObjectValue() {
        return value;
    }

    @Override
    public int getIntegerValue() {
        return value;
    }

    @Override
    public Number numberValue() {
        return value;
    }

    @Override
    public DataTypes getDataType() {
        return DataTypes.MULTISTATE;
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + value;
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
        final MultistateValue other = (MultistateValue) obj;
        if (value != other.value)
            return false;
        return true;
    }

    @Override
    public int compareTo(MultistateValue that) {
        return (value < that.value ? -1 : (value == that.value ? 0 : 1));
    }

    @Override
    public <T extends DataValue> int compareTo(T that) {
        return compareTo((MultistateValue) that);
    }
}
