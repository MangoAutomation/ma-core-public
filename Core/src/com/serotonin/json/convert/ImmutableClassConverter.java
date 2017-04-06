package com.serotonin.json.convert;

import java.lang.reflect.Type;

import com.serotonin.json.JsonContext;
import com.serotonin.json.JsonReader;
import com.serotonin.json.type.JsonValue;

/**
 * A base class for creating converters for immutable objects.
 * 
 * @author Matthew Lohbihler
 */
abstract public class ImmutableClassConverter extends AbstractClassConverter {
    @Override
    protected Object newInstance(JsonContext context, JsonValue jsonValue, Type type) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void jsonRead(JsonReader reader, JsonValue jsonValue, Object obj, Type type) {
        throw new RuntimeException("not implemented");
    }
}
