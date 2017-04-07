package com.serotonin.json.type;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

abstract public class JsonValue {
    public JsonObject toJsonObject() {
        return (JsonObject) this;
    }

    public JsonArray toJsonArray() {
        return (JsonArray) this;
    }

    public JsonValue getJsonValue(String... path) {
        if (path.length == 0)
            return this;
        return getJsonValue(this, path, 0);
    }

    private JsonValue getJsonValue(JsonValue value, String[] path, int index) {
        if (value instanceof JsonObject)
            value = value.toJsonObject().get(path[index]);
        else if (value instanceof JsonArray) {
            int arrIndex = Integer.parseInt(path[index]);
            value = value.toJsonArray().get(arrIndex);
        }
        else
            return null;

        if (value == null || index == path.length - 1)
            return value;

        return getJsonValue(value, path, index + 1);
    }

    public boolean toBoolean() {
        return ((JsonBoolean) this).booleanValue();
    }

    public BigDecimal toNumber() {
        return ((JsonNumber) this).bigDecimalValue();
    }

    public List<Object> toList() {
        throw new ClassCastException(getClass().getName() + " cannot be cast to JsonArray");
    }

    public Map<String, Object> toMap() {
        throw new ClassCastException(getClass().getName() + " cannot be cast to JsonObject");
    }

    public Object toNative() {
        if (this instanceof JsonBoolean)
            return ((JsonBoolean) this).toBoolean();
        if (this instanceof JsonString)
            return ((JsonString) this).toString();
        if (this instanceof JsonNumber)
            return ((JsonNumber) this).bigDecimalValue();
        if (this instanceof JsonArray)
            return toList();
        if (this instanceof JsonObject)
            return toMap();
        return this;
    }
}
