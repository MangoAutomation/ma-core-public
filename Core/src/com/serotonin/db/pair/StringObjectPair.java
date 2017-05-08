package com.serotonin.db.pair;

import java.io.Serializable;

public class StringObjectPair implements Serializable {
    private static final long serialVersionUID = -1;

    private String key;
    private Object value;

    public StringObjectPair() {
        // no op
    }

    public StringObjectPair(String key, Object value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
