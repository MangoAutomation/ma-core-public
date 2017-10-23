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

    /* (non-Javadoc)
     * @see com.serotonin.util.properties.MangoProperties#setDefaultValue(java.lang.String, java.lang.String)
     */
    @Override
    public void setDefaultValue(String key, String value) {
        this.properties.put(key,  value);
    }
}
