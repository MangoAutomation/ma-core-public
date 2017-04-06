package com.serotonin.json.convert;

import java.io.IOException;
import java.lang.reflect.Type;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.JsonWriter;
import com.serotonin.json.type.JsonString;
import com.serotonin.json.type.JsonTypeWriter;
import com.serotonin.json.type.JsonValue;
import com.serotonin.json.util.TypeUtils;

/**
 * Default implementation of an Enum converter
 * 
 * @author Matthew Lohbihler
 */
public class EnumConverter extends ImmutableClassConverter {
    @Override
    public JsonValue jsonWrite(JsonTypeWriter writer, Object value) {
        return new JsonString(value.toString());
    }

    @Override
    public void jsonWrite(JsonWriter writer, Object value) throws IOException {
        writer.quote(value.toString());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Object jsonRead(JsonReader reader, JsonValue jsonValue, Type type) throws JsonException {
        return Enum.valueOf((Class<Enum>) TypeUtils.getRawClass(type), jsonValue.toString());
    }
}
