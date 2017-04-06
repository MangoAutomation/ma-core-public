package com.serotonin.json.factory;

import java.util.HashSet;

import com.serotonin.json.spi.ObjectFactory;
import com.serotonin.json.type.JsonValue;

/**
 * Default implementation of a Set factory. Creates a HashSet
 * 
 * @author Matthew Lohbihler
 */
public class SetFactory implements ObjectFactory {
    @Override
    public Object create(JsonValue jsonValue) {
        return new HashSet<Object>();
    }
}
