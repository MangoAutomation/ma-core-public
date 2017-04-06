package com.serotonin.json.convert;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.JsonWriter;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonTypeWriter;
import com.serotonin.json.type.JsonValue;
import com.serotonin.json.util.TypeUtils;

/**
 * Default implementation of a Map converter
 * 
 * @author Matthew Lohbihler
 */
public class MapConverter extends AbstractClassConverter {
    @Override
    public JsonValue jsonWrite(JsonTypeWriter writer, Object value) throws JsonException {
        Map<?, ?> map = (Map<?, ?>) value;
        JsonObject jsonObject = new JsonObject();
        for (Map.Entry<?, ?> entry : map.entrySet())
            jsonObject.put(entry.getKey().toString(), writer.writeObject(entry.getValue()));
        return jsonObject;
    }

    @Override
    public void jsonWrite(JsonWriter writer, Object value) throws IOException, JsonException {
        Map<?, ?> map = (Map<?, ?>) value;
        ObjectWriter objectWriter = new ObjectJsonWriter(writer);
        for (Map.Entry<?, ?> entry : map.entrySet())
            objectWriter.writeEntry(entry.getKey().toString(), entry.getValue());
        objectWriter.finish();
    }

    @Override
    public void jsonRead(JsonReader reader, JsonValue jsonValue, Object obj, Type type) throws JsonException {
        JsonObject jsonObject = (JsonObject) jsonValue;

        @SuppressWarnings("unchecked")
        Map<Object, Object> map = (Map<Object, Object>) obj;

        Type valueType = TypeUtils.getActualTypeArgument(type, 1);

        for (Map.Entry<String, JsonValue> entry : jsonObject.entrySet())
            map.put(entry.getKey(), reader.read(valueType, entry.getValue()));
    }
}
