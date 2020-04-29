/**
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.json.convert;

import java.io.IOException;
import java.lang.reflect.Type;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.json.JsonContext;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.JsonWriter;
import com.serotonin.json.type.JsonStreamedArray;
import com.serotonin.json.type.JsonTypeWriter;
import com.serotonin.json.type.JsonValue;

/**
 * Stream array values from a source
 *
 * @author Terry Packer
 */
public class StreamedArrayConverter extends AbstractClassConverter {
    @Override
    public JsonValue jsonWrite(JsonTypeWriter writer, Object value) throws JsonException {
        throw new JsonException("Writing full object not supported");
    }

    @Override
    public void jsonWrite(JsonWriter writer, Object value) throws IOException, JsonException {
        writer.append('[');
        writer.increaseIndent();
        ((JsonStreamedArray)value).writeArrayValues(writer);
        writer.decreaseIndent();
        writer.indent();
        writer.append(']');
    }

    @Override
    protected Object newInstance(JsonContext context, JsonValue jsonValue, Type type) {
        throw new ShouldNeverHappenException("Reading not supported");
    }

    @Override
    public void jsonRead(JsonReader reader, JsonValue jsonValue, Object array, Type type) throws JsonException {
        throw new JsonException("Reading not supported");
    }
}
