package com.serotonin.db.pair;

import java.io.Serializable;

public class LongLongPair implements Serializable {
    private static final long serialVersionUID = -1;

    private long key;
    private long value;

    public LongLongPair() {
        // no op
    }

    public LongLongPair(long key, long value) {
        this.key = key;
        this.value = value;
    }

    public void setKey(long key) {
        this.key = key;
    }

    public void setValue(long value) {
        this.value = value;
    }

    public long getKey() {
        return key;
    }

    public long getValue() {
        return value;
    }
}
