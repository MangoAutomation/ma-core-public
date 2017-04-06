package com.serotonin.json.convert;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.UUID;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.JsonWriter;
import com.serotonin.json.type.JsonString;
import com.serotonin.json.type.JsonTypeWriter;
import com.serotonin.json.type.JsonValue;

/**
 * Default implementation of a UUID converter
 * 
 * @author Matthew Lohbihler
 */
public class UUIDConverter extends ImmutableClassConverter {
    @Override
    public JsonValue jsonWrite(JsonTypeWriter writer, Object value) {
        return new JsonString(value.toString());
    }

    @Override
    public void jsonWrite(JsonWriter writer, Object value) throws IOException {
        writer.quote(value.toString());
    }

    @Override
    public Object jsonRead(JsonReader reader, JsonValue jsonValue, Type type) throws JsonException {
        return UUID.fromString(jsonValue.toString());
    }
}
