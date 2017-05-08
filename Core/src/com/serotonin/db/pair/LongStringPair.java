package com.serotonin.db.pair;

import java.io.Serializable;

public class LongStringPair implements Serializable {
    private static final long serialVersionUID = -1;

    private long key;
    private String value;

    public LongStringPair() {
        // no op
    }

    public LongStringPair(long key, String value) {
        this.key = key;
        this.value = value;
    }

    public void setKey(long key) {
        this.key = key;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public long getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
