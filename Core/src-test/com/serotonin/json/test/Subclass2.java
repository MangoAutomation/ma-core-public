package com.serotonin.json.test;

import com.serotonin.json.spi.JsonProperty;

public class Subclass2 extends BaseClass {
    @JsonProperty
    private String sub2Value;

    public String getSub2Value() {
        return sub2Value;
    }

    public void setSub2Value(String sub2Value) {
        this.sub2Value = sub2Value;
    }

    @Override
    @JsonProperty(alias = "myId")
    public String getId() {
        return "Subclass2";
    }
}
