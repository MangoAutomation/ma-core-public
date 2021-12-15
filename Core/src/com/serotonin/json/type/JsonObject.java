package com.serotonin.json.type;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class JsonObject extends JsonValue implements Map<String, JsonValue> {
    private final Map<String, JsonValue> delegate = new HashMap<>();

    //
    // Map interface
    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return delegate.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return delegate.containsValue(value);
    }

    @Override
    public JsonValue get(Object key) {
        return delegate.get(key);
    }

    @Override
    public JsonValue put(String key, JsonValue value) {
        return delegate.put(key, value);
    }

    @Override
    public JsonValue remove(Object key) {
        return delegate.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ? extends JsonValue> m) {
        delegate.putAll(m);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public Set<String> keySet() {
        return delegate.keySet();
    }

    @Override
    public Collection<JsonValue> values() {
        return delegate.values();
    }

    @Override
    public Set<java.util.Map.Entry<String, JsonValue>> entrySet() {
        return delegate.entrySet();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    //
    // Convenience
    public void put(String key, boolean b) {
        put(key, new JsonBoolean(b));
    }

    public void put(String key, String s) {
        put(key, new JsonString(s));
    }

    public void put(String key, int i) {
        put(key, new JsonNumber(new BigDecimal(i)));
    }

    public void put(String key, long l) {
        put(key, new JsonNumber(new BigDecimal(l)));
    }

    public void put(String key, float f) {
        put(key, new JsonNumber(new BigDecimal(f)));
    }

    public void put(String key, double d) {
        put(key, new JsonNumber(new BigDecimal(d)));
    }

    public void put(String key, BigInteger bi) {
        put(key, new JsonNumber(new BigDecimal(bi)));
    }

    public JsonObject getJsonObject(String key) {
        return (JsonObject) delegate.get(key);
    }

    public JsonArray getJsonArray(String key) {
        return (JsonArray) delegate.get(key);
    }

    public JsonNumber getJsonNumber(String key) {
        return (JsonNumber) delegate.get(key);
    }

    public JsonBoolean getJsonBoolean(String key) {
        return (JsonBoolean) delegate.get(key);
    }

    public JsonString getJsonString(String key) {
        return (JsonString) delegate.get(key);
    }

    /**
     * Deprecated, use AbstractVO.getBoolean(json, key) instead
     */
    @Deprecated 
    public boolean getBoolean(String key) {
        return getJsonBoolean(key).toBoolean();
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        JsonBoolean jb = getJsonBoolean(key);
        if (jb == null)
            return defaultValue;
        return jb.toBoolean();
    }

    public String getString(String key) {
        JsonString js = getJsonString(key);
        if (js == null)
            return null;
        return js.toString();
    }
    
    public String getString(String key, String defaultValue) {
        JsonString js = getJsonString(key);
        if (js == null)
            return defaultValue;
        return js.toString(); 
    }

    /**
     * Deprecated, use AbstractVO.getInt(json, key) instead
     */
    @Deprecated 
    public int getInt(String key) {
        return getJsonNumber(key).intValue();
    }

    public int getInt(String key, int defaultValue) {
        JsonNumber jn = getJsonNumber(key);
        if (jn == null)
            return defaultValue;
        return jn.intValue();
    }

    public long getLong(String key) {
        return getJsonNumber(key).longValue();
    }

    public long getLong(String key, long defaultValue) {
        JsonNumber jn = getJsonNumber(key);
        if (jn == null)
            return defaultValue;
        return jn.longValue();
    }

    public float getFloat(String key) {
        return getJsonNumber(key).floatValue();
    }

    public float getFloat(String key, float defaultValue) {
        JsonNumber jn = getJsonNumber(key);
        if (jn == null)
            return defaultValue;
        return jn.floatValue();
    }

    /**
     * Deprecated, use AbstractVO.getDouble(json, key) instead
     */
    @Deprecated 
    public double getDouble(String key) {
        return getJsonNumber(key).doubleValue();
    }

    public double getDouble(String key, double defaultValue) {
        JsonNumber jn = getJsonNumber(key);
        if (jn == null)
            return defaultValue;
        return jn.doubleValue();
    }

    public BigInteger getBigInteger(String key) {
        return getJsonNumber(key).bigIntegerValue();
    }

    public BigDecimal getBigDecimal(String key) {
        return getJsonNumber(key).bigDecimalValue();
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        for (Map.Entry<String, JsonValue> e : delegate.entrySet())
            map.put(e.getKey(), e.getValue() == null ? null : e.getValue().toNative());
        return map;
    }
}
