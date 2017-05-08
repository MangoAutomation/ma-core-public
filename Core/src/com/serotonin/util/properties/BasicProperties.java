package com.serotonin.util.properties;

import java.util.Map;

public class BasicProperties extends AbstractProperties {
    private final Map<String, String> properties;

    public BasicProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public BasicProperties(String description, Map<String, String> properties) {
        super(description);
        this.properties = properties;
    }

    @Override
    protected String getStringImpl(String key) {
        return properties.get(key);
    }
}
