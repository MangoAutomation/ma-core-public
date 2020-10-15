/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.serotonin.json.convert;

import java.io.IOException;
import java.lang.reflect.Type;

import com.infiniteautomation.mango.util.LazyField;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.JsonWriter;
import com.serotonin.json.type.JsonTypeWriter;
import com.serotonin.json.type.JsonValue;
import com.serotonin.json.util.TypeUtils;

/**
 *
 * @author Terry Packer
 */
public class LazyFieldConverter extends AbstractClassConverter {

    @Override
    public JsonValue jsonWrite(JsonTypeWriter writer, Object value) throws JsonException {
        LazyField<?> field = (LazyField<?>)value;
        Object lazyValue = field.get();
        return writer.writeObject(lazyValue);
    }

    @Override
    public void jsonWrite(JsonWriter writer, Object value) throws IOException, JsonException {
        LazyField<?> field = (LazyField<?>)value;
        Object lazyValue = field.get();
        writer.writeObject(lazyValue);
    }

    @Override
    public void jsonRead(JsonReader reader, JsonValue jsonValue, Object obj, Type type)
            throws JsonException {
        @SuppressWarnings("unchecked")
        LazyField<Object> field = (LazyField<Object>)obj;
        Type innerType = TypeUtils.getActualTypeArgument(type, 0);

        field.set(reader.read(innerType, jsonValue));
    }

}
