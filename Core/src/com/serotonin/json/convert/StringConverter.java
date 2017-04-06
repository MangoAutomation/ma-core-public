package com.serotonin.json.convert;

import java.io.IOException;
import java.lang.reflect.Type;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.JsonWriter;
import com.serotonin.json.type.JsonString;
import com.serotonin.json.type.JsonTypeWriter;
import com.serotonin.json.type.JsonValue;

/**
 * Default implementation of a String converter
 * 
 * @author Matthew Lohbihler
 */
public class StringConverter extends ImmutableClassConverter {
    @Override
    public JsonValue jsonWrite(JsonTypeWriter writer, Object value) {
        return new JsonString((String) value);
    }

    @Override
    public void jsonWrite(JsonWriter writer, Object object) throws IOException {
        writer.quote((String) object);
    }

    @Override
    public Object jsonRead(JsonReader reader, JsonValue jsonValue, Type type) throws JsonException {
        return jsonValue.toString();
    }
}
