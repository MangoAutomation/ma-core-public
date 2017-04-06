package com.serotonin.json.junit.vo;

public class GenObject<T> {
    private T value;

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }
}
