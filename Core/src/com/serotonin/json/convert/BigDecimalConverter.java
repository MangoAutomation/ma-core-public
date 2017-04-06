package com.serotonin.json.convert;

import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.JsonWriter;
import com.serotonin.json.type.JsonNumber;
import com.serotonin.json.type.JsonTypeWriter;
import com.serotonin.json.type.JsonValue;

/**
 * Default implementation of a BigDecimal converter
 * 
 * @author Matthew Lohbihler
 */
public class BigDecimalConverter extends ImmutableClassConverter {
    @Override
    public JsonValue jsonWrite(JsonTypeWriter writer, Object value) {
        return new JsonNumber((BigDecimal) value);
    }

    @Override
    public void jsonWrite(JsonWriter writer, Object value) throws IOException {
        writer.append(value.toString());
    }

    @Override
    public Object jsonRead(JsonReader reader, JsonValue jsonValue, Type type) throws JsonException {
        return ((JsonNumber) jsonValue).bigDecimalValue();
    }
}
