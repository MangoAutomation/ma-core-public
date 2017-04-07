package com.serotonin.json.factory;

import java.util.HashMap;

import com.serotonin.json.spi.ObjectFactory;
import com.serotonin.json.type.JsonValue;

/**
 * Default implementation of a Map factory. Creates a HashMap.
 * 
 * @author Matthew Lohbihler
 */
public class MapFactory implements ObjectFactory {
    @Override
    public Object create(JsonValue jsonValue) {
        return new HashMap<>();
    }
}
