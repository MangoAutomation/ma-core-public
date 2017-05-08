package com.serotonin.util.properties;

abstract public class MutableProperties extends AbstractProperties {
    public void setString(String key, String value) {
        setStringImpl(key, value);
    }

    public void setInt(String key, int value) {
        setStringImpl(key, Integer.toString(value));
    }

    public void setLong(String key, long value) {
        setStringImpl(key, Long.toString(value));
    }

    public void setBoolean(String key, boolean value) {
        setStringImpl(key, Boolean.toString(value));
    }

    public void setDouble(String key, double value) {
        setStringImpl(key, Double.toString(value));
    }

    abstract protected void setStringImpl(String key, String value);
}
