package com.serotonin.json.test;

import com.serotonin.json.spi.JsonProperty;

public class Mutable extends Immutable {
    @JsonProperty
    public void setS1(String s1) {
        this.s1 = s1;
    }

    @JsonProperty
    public void setS2(String s2) {
        this.s2 = s2;
    }
}
