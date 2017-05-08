package com.serotonin.db.pair;

import java.io.Serializable;

public class IntStringPair implements Serializable {
    private static final long serialVersionUID = -1;

    private int key;
    private String value;

    public IntStringPair() {
        // no op
    }

    public IntStringPair(int key, String value) {
        this.key = key;
        this.value = value;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
