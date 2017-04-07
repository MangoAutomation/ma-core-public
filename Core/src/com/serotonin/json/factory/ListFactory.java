package com.serotonin.json.factory;

import java.util.ArrayList;

import com.serotonin.json.spi.ObjectFactory;
import com.serotonin.json.type.JsonValue;

/**
 * Default implementation of a List factory. Creates an ArrayList.
 * 
 * @author Matthew Lohbihler
 */
public class ListFactory implements ObjectFactory {
    @Override
    public Object create(JsonValue jsonValue) {
        return new ArrayList<>();
    }
}
