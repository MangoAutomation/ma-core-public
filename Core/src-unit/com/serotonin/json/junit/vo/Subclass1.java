package com.serotonin.json.junit.vo;

public class Subclass1 extends BaseClass {
    private String sub1Value;

    public String getSub1Value() {
        return sub1Value;
    }

    public void setSub1Value(String sub1Value) {
        this.sub1Value = sub1Value;
    }

    @Override
    public String getId() {
        return "Subclass1";
    }
}
