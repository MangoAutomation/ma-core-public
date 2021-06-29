/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.dataImage.types;

import com.serotonin.m2m2.DataTypes;

/**
 * @author Matthew Lohbihler
 */
public class BinaryValue extends DataValue implements Comparable<BinaryValue> {
    public static final BinaryValue ZERO = new BinaryValue(false);
    public static final BinaryValue ONE = new BinaryValue(true);

    public static BinaryValue parseBinary(String s) {
        if (s == null || "0".equals(s))
            return ZERO;
        if ("1".equals(s) || Boolean.parseBoolean(s))
            return ONE;
        return ZERO;
    }

    private final boolean value;

    public BinaryValue(boolean value) {
        this.value = value;
    }

    @Override
    public boolean hasDoubleRepresentation() {
        return true;
    }

    @Override
    public double getDoubleValue() {
        return value ? 1 : 0;
    }

    @Override
    public String getStringValue() {
        return null;
    }

    @Override
    public boolean getBooleanValue() {
        return value;
    }

    @Override
    public Object getObjectValue() {
        return value;
    }

    @Override
    public int getIntegerValue() {
        return value ? 1 : 0;
    }

    @Override
    public int getDataType() {
        return DataTypes.BINARY;
    }

    @Override
    public String toString() {
        return Boolean.toString(value);
    }

    @Override
    public Number numberValue() {
        throw new RuntimeException("BinaryValue has no Number value.");
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (value ? 1231 : 1237);
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
        final BinaryValue other = (BinaryValue) obj;
        if (value != other.value)
            return false;
        return true;
    }

    @Override
    public int compareTo(BinaryValue that) {
        return (that.value == value ? 0 : (value ? 1 : -1));
    }

    @Override
    public <T extends DataValue> int compareTo(T that) {
        return compareTo((BinaryValue) that);
    }
}
