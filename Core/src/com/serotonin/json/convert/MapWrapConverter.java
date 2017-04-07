package com.serotonin.json.convert;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.JsonWriter;
import com.serotonin.json.spi.ClassConverter;
import com.serotonin.json.type.JsonTypeWriter;
import com.serotonin.json.type.JsonValue;
import com.serotonin.json.util.TypeDefinition;
import com.serotonin.util.MapWrap;

public class MapWrapConverter implements ClassConverter {
    private final MapConverter mapConverter = new MapConverter();

    @Override
    public JsonValue jsonWrite(JsonTypeWriter writer, Object value) throws JsonException {
        return mapConverter.jsonWrite(writer, ((MapWrap) value).getInternalMap());
    }

    @Override
    public void jsonWrite(JsonWriter writer, Object value) throws IOException, JsonException {
        mapConverter.jsonWrite(writer, ((MapWrap) value).getInternalMap());
    }

    @Override
    public Object jsonRead(JsonReader reader, JsonValue jsonValue, Type type) throws JsonException {
        return new MapWrap(mapConverter.jsonRead(reader, jsonValue, new TypeDefinition(Map.class, String.class,
                Object.class)));
    }

    @Override
    public void jsonRead(JsonReader reader, JsonValue jsonValue, Object obj, Type type) throws JsonException {
        mapConverter.jsonRead(reader, jsonValue, ((MapWrap) obj).getInternalMap(), new TypeDefinition(Map.class,
                String.class, Object.class));
    }
}
