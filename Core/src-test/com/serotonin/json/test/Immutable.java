package com.serotonin.json.test;

import com.serotonin.json.spi.JsonProperty;

public class Immutable {
    @JsonProperty
    String s1;
    @JsonProperty
    String s2;

    public String getS1() {
        return s1;
    }

    public String getS2() {
        return s2;
    }
}
