package com.serotonin.util;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public class MapWrap implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<String, Object> map;

    public MapWrap() {
        map = new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    public MapWrap(Object o) {
        this((Map<String, Object>) o);
    }

    public MapWrap(Map<String, Object> map) {
        this.map = map;
    }

    public boolean containsKey(String key) {
        return map.containsKey(key);
    }

    public Object get(String key) {
        return map.get(key);
    }

    public MapWrap getMap(String key) {
        Object o = get(key);
        if (o == null)
            return null;
        return (MapWrap) o;
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getList(String key) {
        Object o = get(key);
        if (o == null)
            return null;
        return (List<T>) o;
    }

    public List<MapWrap> getListOfMaps(String key) {
        List<MapWrap> list = getList(key);
        return list;
    }

    public int[] getListAsIntArray(String key) {
        List<Number> list = getList(key);
        int[] arr = new int[list.size()];
        for (int i = 0; i < arr.length; i++)
            arr[i] = list.get(i).intValue();
        return arr;
    }

    @SuppressWarnings("unchecked")
    public String[] getStringArray(String key) {
        Object o = get(key);
        if (o == null)
            return null;
        if (o instanceof List) {
            List<String> list = (List<String>) o;
            String[] arr = new String[list.size()];
            list.toArray(arr);
            return arr;
        }
        if (o instanceof String) {
            String s = (String) o;
            if (StringUtils.isEmpty(s))
                return new String[0];
            return ((String) o).split(",");
        }
        throw new RuntimeException(o.getClass().toString());
    }

    public String getString(String key) {
        return (String) get(key);
    }

    public boolean getBoolean(String key) {
        Object o = get(key);
        if (o instanceof Boolean)
            return (Boolean) o;
        if (o instanceof String)
            return "true".equals(o);
        throw new RuntimeException(o == null ? "null" : o.getClass().toString());
    }

    public boolean getBoolean(String key, boolean def) {
        Object o = get(key);
        if (o instanceof Boolean)
            return (Boolean) o;
        if (o instanceof String)
            return "true".equals(o);
        return def;
    }

    public int getInt(String key) {
        Object o = get(key);
        if (o instanceof Number)
            return ((Number) o).intValue();
        try {
            if (o instanceof String)
                return Integer.parseInt((String) o);
        }
        catch (NumberFormatException e) {
            return 0;
        }
        throw new RuntimeException("For key '" + key + "':" + (o == null ? "null" : o.getClass().toString()));
    }

    public int getInt(String key, int def) {
        Object o = get(key);
        if (o instanceof Number)
            return ((Number) o).intValue();
        try {
            if (o instanceof String)
                return Integer.parseInt((String) o);
        }
        catch (NumberFormatException e) {
            // no op
        }
        return def;
    }

    public long getLong(String key) {
        Object o = get(key);
        if (o instanceof Number)
            return ((Number) o).longValue();
        try {
            if (o instanceof String)
                return Long.parseLong((String) o);
        }
        catch (NumberFormatException e) {
            return 0;
        }
        throw new RuntimeException(o == null ? "null" : o.getClass().toString());
    }

    public long getLong(String key, long def) {
        Object o = get(key);
        if (o instanceof Number)
            return ((Number) o).longValue();
        try {
            if (o instanceof String)
                return Long.parseLong((String) o);
        }
        catch (NumberFormatException e) {
            // no op
        }
        return def;
    }

    public double getDouble(String key) {
        Object o = get(key);
        if (o instanceof Number)
            return ((Number) o).doubleValue();
        try {
            if (o instanceof String)
                return Double.parseDouble((String) o);
        }
        catch (NumberFormatException e) {
            return 0;
        }
        throw new RuntimeException(o == null ? "null" : o.getClass().toString());
    }

    public double getDouble(String key, double def) {
        Object o = get(key);
        if (o instanceof Number)
            return ((Number) o).doubleValue();
        try {
            if (o instanceof String)
                return Double.parseDouble((String) o);
        }
        catch (NumberFormatException e) {
            // no op
        }
        return def;
    }

    public void put(String key, Object value) {
        map.put(key, value);
    }

    public void putAll(MapWrap that) {
        map.putAll(that.map);
    }

    public Object remove(String key) {
        return map.remove(key);
    }

    public MapWrap removeMap(String key) {
        MapWrap o = getMap(key);
        map.remove(key);
        return o;
    }

    public <T> List<T> removeList(String key) {
        List<T> o = getList(key);
        map.remove(key);
        return o;
    }

    public List<MapWrap> removeListOfMaps(String key) {
        List<MapWrap> o = getListOfMaps(key);
        map.remove(key);
        return o;
    }

    public int[] removeListAsIntArray(String key) {
        int[] o = getListAsIntArray(key);
        map.remove(key);
        return o;
    }

    public String[] removeStringArray(String key) {
        String[] o = getStringArray(key);
        map.remove(key);
        return o;
    }

    public String removeString(String key) {
        return (String) remove(key);
    }

    public boolean removeBoolean(String key) {
        boolean o = getBoolean(key);
        map.remove(key);
        return o;
    }

    public boolean removeBoolean(String key, boolean def) {
        boolean o = getBoolean(key, def);
        map.remove(key);
        return o;
    }

    public int removeInt(String key) {
        int o = getInt(key);
        map.remove(key);
        return o;
    }

    public int removeInt(String key, int def) {
        int o = getInt(key, def);
        map.remove(key);
        return o;
    }

    public long removeLong(String key) {
        long o = getLong(key);
        map.remove(key);
        return o;
    }

    public long removeLong(String key, long def) {
        long o = getLong(key, def);
        map.remove(key);
        return o;
    }

    public double removeDouble(String key) {
        double o = getDouble(key);
        map.remove(key);
        return o;
    }

    public double removeDouble(String key, double def) {
        double o = getDouble(key, def);
        map.remove(key);
        return o;
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public String toString() {
        return map.toString();
    }

    public Map<String, Object> getInternalMap() {
        return map;
    }
}
