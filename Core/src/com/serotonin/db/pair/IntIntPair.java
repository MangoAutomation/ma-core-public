package com.serotonin.db.pair;

import java.io.Serializable;

public class IntIntPair implements Serializable {
    private static final long serialVersionUID = -1;

    private int key;
    private int value;

    public IntIntPair() {
        // no op
    }

    public IntIntPair(int key, int value) {
        this.key = key;
        this.value = value;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public int getKey() {
        return key;
    }

    public int getValue() {
        return value;
    }
}
