/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.dataImage.types;

import java.util.Objects;

import com.serotonin.InvalidArgumentException;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.DataType;

/**
 * @author Matthew Lohbihler
 */
abstract public class DataValue {
    public static DataValue stringToValue(String valueStr, DataType dataType) {
        switch (dataType) {
        case BINARY:
            return BinaryValue.parseBinary(valueStr);
        case MULTISTATE:
            return MultistateValue.parseMultistate(valueStr);
        case NUMERIC:
            return NumericValue.parseNumeric(valueStr);
        case IMAGE:
            try {
                return new ImageValue(valueStr);
            }
            catch (InvalidArgumentException e) {
                // no op
            }
            return null;
        case ALPHANUMERIC:
            return new AlphanumericValue(valueStr);
        }
        throw new ShouldNeverHappenException("Invalid data type " + dataType + ". Cannot instantiate DataValue");
    }

    public static DataValue objectToValue(Object value) {
        if (value instanceof Boolean)
            return new BinaryValue((Boolean) value);
        if (value instanceof Integer)
            return new MultistateValue((Integer) value);
        if (value instanceof Double)
            return new NumericValue((Double) value);
        if (value instanceof String)
            return new AlphanumericValue((String) value);
        throw new ShouldNeverHappenException("Unrecognized object type " + (value == null ? "null" : value.getClass())
                + ". Cannot instantiate DataValue");
    }

    public static boolean isEqual(DataValue v1, DataValue v2) {
        return Objects.equals(v1, v2);
    }

    abstract public boolean hasDoubleRepresentation();

    abstract public double getDoubleValue();

    abstract public String getStringValue();

    abstract public int getIntegerValue();

    abstract public boolean getBooleanValue();

    abstract public Object getObjectValue();

    abstract public DataType getDataType();

    abstract public Number numberValue();

    public static Number numberValue(DataValue value) {
        if (value == null)
            return null;
        return value.numberValue();
    }

    abstract public <T extends DataValue> int compareTo(T that);
}
