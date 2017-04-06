package com.serotonin.json.convert;

import java.io.IOException;
import java.lang.reflect.Type;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.JsonWriter;
import com.serotonin.json.type.JsonBoolean;
import com.serotonin.json.type.JsonTypeWriter;
import com.serotonin.json.type.JsonValue;

/**
 * Default implementation of a Boolean converter
 * 
 * @author Matthew Lohbihler
 */
public class BooleanConverter extends ImmutableClassConverter {
    @Override
    public JsonValue jsonWrite(JsonTypeWriter writer, Object value) {
        return new JsonBoolean((Boolean) value);
    }

    @Override
    public void jsonWrite(JsonWriter writer, Object value) throws IOException {
        writer.append(value.toString());
    }

    @Override
    public Object jsonRead(JsonReader reader, JsonValue jsonValue, Type type) throws JsonException {
        return ((JsonBoolean) jsonValue).toBoolean();
    }
}
