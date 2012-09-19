package com.serotonin.db;

import java.io.Serializable;

/**
 * @deprecated use com.serotonin.db.pair.IntStringPair instead
 */
@Deprecated
public class IntValuePair implements Serializable {
    private static final long serialVersionUID = -1;

    private int key;
    private String value;

    public IntValuePair() {
        // no op
    }

    public IntValuePair(int key, String value) {
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
