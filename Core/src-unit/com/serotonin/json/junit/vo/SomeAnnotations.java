package com.serotonin.json.junit.vo;

import com.serotonin.json.spi.JsonProperty;

/**
 * @author Matthew Lohbihler
 */
public class SomeAnnotations {
    @JsonProperty
    private int id;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDesc() {
        return "description";
    }
}
