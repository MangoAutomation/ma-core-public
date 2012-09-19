package com.serotonin.db;

import java.io.Serializable;

/**
 * @deprecated use com.serotonin.db.pair.StringStringPair instead
 */
@Deprecated
public class KeyValuePair implements Serializable {
    private static final long serialVersionUID = -1;

    private String key;
    private String value;

    public KeyValuePair() {
        // no op
    }

    public KeyValuePair(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
