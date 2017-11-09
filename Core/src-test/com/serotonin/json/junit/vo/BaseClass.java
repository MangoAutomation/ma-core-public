package com.serotonin.json.junit.vo;

abstract public class BaseClass {
    private String baseValue;

    public String getBaseValue() {
        return baseValue;
    }

    public void setBaseValue(String baseValue) {
        this.baseValue = baseValue;
    }

    abstract public String getId();
}
