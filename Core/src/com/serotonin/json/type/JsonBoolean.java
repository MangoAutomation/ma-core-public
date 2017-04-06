package com.serotonin.json.type;

public class JsonBoolean extends JsonValue {
    private final boolean delegate;

    public JsonBoolean(boolean b) {
        this.delegate = b;
    }

    public boolean booleanValue() {
        return delegate;
    }

    @Override
    public String toString() {
        return Boolean.toString(delegate);
    }
}
