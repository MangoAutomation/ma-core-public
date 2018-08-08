/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2;

import java.util.HashMap;
import java.util.Map;

import com.serotonin.util.properties.AbstractProperties;

/**
 * Dummy implementation of properties for use in testing.
 *
 * @author Terry Packer
 */
public class MockMangoProperties extends AbstractProperties{

    protected Map<String, String> properties = new HashMap<>();
    
    public MockMangoProperties() {
        //Fill in all default values for properties
        properties.put("db.update.log.dir", Common.MA_HOME + "/logs/");
        properties.put("security.hashAlgorithm", "NONE");
    }
    
    /* (non-Javadoc)
     * @see com.serotonin.util.properties.MangoProperties#setDefaultValue(java.lang.String, java.lang.String)
     */
    @Override
    public void setDefaultValue(String key, String value) {
        properties.put(key, value);   
    }

    /* (non-Javadoc)
     * @see com.serotonin.util.properties.AbstractProperties#getStringImpl(java.lang.String)
     */
    @Override
    protected String getStringImpl(String key) {
        return properties.get(key);
    }
    
    
    
  

}
