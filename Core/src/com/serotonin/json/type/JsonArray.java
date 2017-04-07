package com.serotonin.json.type;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class JsonArray extends JsonValue implements List<JsonValue> {
    private final List<JsonValue> delegate = new ArrayList<>();

    //
    // List interface
    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return delegate.contains(o);
    }

    @Override
    public Iterator<JsonValue> iterator() {
        return delegate.iterator();
    }

    @Override
    public Object[] toArray() {
        return delegate.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return delegate.toArray(a);
    }

    @Override
    public boolean add(JsonValue e) {
        return delegate.add(e);
    }

    @Override
    public boolean remove(Object o) {
        return delegate.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return delegate.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends JsonValue> c) {
        return delegate.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends JsonValue> c) {
        return delegate.addAll(index, c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return delegate.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return delegate.removeAll(c);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public JsonValue get(int index) {
        return delegate.get(index);
    }

    @Override
    public JsonValue set(int index, JsonValue element) {
        return delegate.set(index, element);
    }

    @Override
    public void add(int index, JsonValue element) {
        delegate.add(index, element);
    }

    @Override
    public JsonValue remove(int index) {
        return delegate.remove(index);
    }

    @Override
    public int indexOf(Object o) {
        return delegate.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return delegate.lastIndexOf(o);
    }

    @Override
    public ListIterator<JsonValue> listIterator() {
        return delegate.listIterator();
    }

    @Override
    public ListIterator<JsonValue> listIterator(int index) {
        return delegate.listIterator(index);
    }

    @Override
    public List<JsonValue> subList(int fromIndex, int toIndex) {
        return delegate.subList(fromIndex, toIndex);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    //
    // Convenience
    public void add(boolean b) {
        add(new JsonBoolean(b));
    }

    public void add(String s) {
        add(new JsonString(s));
    }

    public void add(int i) {
        add(new JsonNumber(new BigDecimal(i)));
    }

    public void add(long l) {
        add(new JsonNumber(new BigDecimal(l)));
    }

    public void add(float f) {
        add(new JsonNumber(new BigDecimal(f)));
    }

    public void add(double d) {
        add(new JsonNumber(new BigDecimal(d)));
    }

    public void add(BigInteger bi) {
        add(new JsonNumber(new BigDecimal(bi)));
    }

    public JsonObject getJsonObject(int index) {
        return (JsonObject) delegate.get(index);
    }

    public JsonArray getJsonArray(int index) {
        return (JsonArray) delegate.get(index);
    }

    public JsonNumber getJsonNumber(int index) {
        return (JsonNumber) delegate.get(index);
    }

    public JsonBoolean getJsonBoolean(int index) {
        return (JsonBoolean) delegate.get(index);
    }

    public JsonString getJsonString(int index) {
        return (JsonString) delegate.get(index);
    }

    public boolean getBoolean(int index) {
        JsonBoolean jb = getJsonBoolean(index);
        if (jb == null)
            return false;
        return jb.toBoolean();
    }

    public String getString(int index) {
        JsonString js = getJsonString(index);
        if (js == null)
            return null;
        return js.toString();
    }

    public int getInt(int index) {
        JsonNumber jn = getJsonNumber(index);
        if (jn == null)
            return 0;
        return jn.intValue();
    }

    public long getLong(int index) {
        JsonNumber jn = getJsonNumber(index);
        if (jn == null)
            return 0;
        return jn.longValue();
    }

    public float getFloat(int index) {
        JsonNumber jn = getJsonNumber(index);
        if (jn == null)
            return 0;
        return jn.floatValue();
    }

    public double getDouble(int index) {
        JsonNumber jn = getJsonNumber(index);
        if (jn == null)
            return 0;
        return jn.doubleValue();
    }

    public BigInteger getBigInteger(int index) {
        JsonNumber jn = getJsonNumber(index);
        if (jn == null)
            return new BigInteger("0");
        return jn.bigIntegerValue();
    }

    public BigDecimal getBigDecimal(int index) {
        JsonNumber jn = getJsonNumber(index);
        if (jn == null)
            return new BigDecimal(0);
        return jn.bigDecimalValue();
    }

    @Override
    public List<Object> toList() {
        List<Object> list = new ArrayList<>();
        for (JsonValue e : delegate)
            list.add(e == null ? null : e.toNative());
        return list;
    }
}
