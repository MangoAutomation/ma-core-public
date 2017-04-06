package com.serotonin.json.convert;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Iterator;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.JsonWriter;
import com.serotonin.json.type.JsonArray;
import com.serotonin.json.type.JsonTypeWriter;
import com.serotonin.json.type.JsonValue;
import com.serotonin.json.util.TypeUtils;

/**
 * Default implementation of a Collection converter
 * 
 * @author Matthew Lohbihler
 */
public class CollectionConverter extends AbstractClassConverter {
    @Override
    public JsonValue jsonWrite(JsonTypeWriter writer, Object value) throws JsonException {
        JsonArray jsonArray = new JsonArray();

        Iterator<?> iterator = ((Collection<?>) value).iterator();
        while (iterator.hasNext()) {
            Object o = iterator.next();
            jsonArray.add(writer.writeObject(o));
        }

        return jsonArray;
    }

    @Override
    public void jsonWrite(JsonWriter writer, Object object) throws IOException, JsonException {
        Iterator<?> iterator = ((Collection<?>) object).iterator();

        writer.append('[');
        writer.increaseIndent();
        boolean first = true;
        while (iterator.hasNext()) {
            Object o = iterator.next();
            if (first)
                first = false;
            else
                writer.append(',');
            writer.indent();
            writer.writeObject(o);
        }
        writer.decreaseIndent();
        writer.indent();
        writer.append(']');
    }

    @Override
    public void jsonRead(JsonReader reader, JsonValue jsonValue, Object obj, Type type) throws JsonException {
        JsonArray jsonArray = (JsonArray) jsonValue;

        @SuppressWarnings("unchecked")
        Collection<Object> collection = (Collection<Object>) obj;

        Type innerType = TypeUtils.getActualTypeArgument(type, 0);

        for (JsonValue element : jsonArray)
            collection.add(reader.read(innerType, element));
    }
}
