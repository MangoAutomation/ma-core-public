package com.serotonin.json.type;

public class JsonString extends JsonValue {
    private final String delegate;

    public JsonString(String s) {
        this.delegate = s;
    }

    @Override
    public String toString() {
        return delegate;
    }
}
