package com.serotonin.json.convert;

import java.lang.reflect.Type;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonValue;

/**
 * Default implementation of a JsonString converter
 * 
 * @author Matthew Lohbihler
 */
public class JsonObjectConverter extends MapConverter {
    @Override
    public Object jsonRead(JsonReader reader, JsonValue jsonValue, Type type) {
        return jsonValue;
    }

    @Override
    public void jsonRead(JsonReader reader, JsonValue jsonValue, Object obj, Type type) throws JsonException {
        JsonObject jsonObject = (JsonObject) obj;
        jsonObject.clear();
        jsonObject.putAll(jsonValue.toJsonObject());
    }
}
