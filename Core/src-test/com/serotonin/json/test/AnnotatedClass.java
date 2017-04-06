package com.serotonin.json.test;

import com.serotonin.json.spi.JsonProperty;

public class AnnotatedClass {
    @JsonProperty(read = false)
    private String id;
    @JsonProperty(write = false)
    private String name;

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id
     *            the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     *            the name to set
     */
    public void setName(String name) {
        this.name = name;
    }
}
