package com.serotonin.json.convert;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Type;

import com.serotonin.json.JsonContext;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.JsonWriter;
import com.serotonin.json.type.JsonArray;
import com.serotonin.json.type.JsonTypeWriter;
import com.serotonin.json.type.JsonValue;
import com.serotonin.json.util.TypeUtils;

/**
 * Default implementation of an Array converter
 * 
 * @author Matthew Lohbihler
 */
public class ArrayConverter extends AbstractClassConverter {
    @Override
    public JsonValue jsonWrite(JsonTypeWriter writer, Object value) throws JsonException {
        int length = Array.getLength(value);
        JsonArray jsonArray = new JsonArray();
        for (int i = 0; i < length; i++)
            jsonArray.add(writer.writeObject(Array.get(value, i)));
        return jsonArray;
    }

    @Override
    public void jsonWrite(JsonWriter writer, Object value) throws IOException, JsonException {
        writer.append('[');
        writer.increaseIndent();
        int length = Array.getLength(value);
        for (int i = 0; i < length; i++) {
            if (i > 0)
                writer.append(',');
            writer.indent();
            writer.writeObject(Array.get(value, i));
        }
        writer.decreaseIndent();
        writer.indent();
        writer.append(']');
    }

    @Override
    protected Object newInstance(JsonContext context, JsonValue jsonValue, Type type) {
        JsonArray jsonArray = (JsonArray) jsonValue;
        Class<?> clazz = TypeUtils.getRawClass(type).getComponentType();
        return Array.newInstance(clazz, jsonArray.size());
    }

    @Override
    public void jsonRead(JsonReader reader, JsonValue jsonValue, Object array, Type type) throws JsonException {
        JsonArray jsonArray = (JsonArray) jsonValue;
        Class<?> clazz = array.getClass().getComponentType();

        for (int i = 0; i < jsonArray.size(); i++) {
            try {
                Array.set(array, i, reader.read(clazz, jsonArray.get(i)));
            }
            catch (Exception e) {
                throw new JsonException("JsonException reading array element " + i, e);
            }
        }
    }
}
