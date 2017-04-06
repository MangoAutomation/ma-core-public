package com.serotonin.json.convert;

import java.lang.reflect.Type;

import com.serotonin.json.JsonContext;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.spi.ClassConverter;
import com.serotonin.json.type.JsonValue;
import com.serotonin.json.util.TypeUtils;

/**
 * A base class for creating converters.
 * 
 * @author Matthew Lohbihler
 */
abstract public class AbstractClassConverter implements ClassConverter {
    @Override
    public Object jsonRead(JsonReader reader, JsonValue jsonValue, Type type) throws JsonException {
        Object obj = newInstance(reader.getContext(), jsonValue, type);
        jsonRead(reader, jsonValue, obj, type);
        return obj;
    }

    protected Object newInstance(JsonContext context, JsonValue jsonValue, Type type) throws JsonException {
        return context.getNewInstance(TypeUtils.getRawClass(type), jsonValue);
    }
}
