/*
 * Copyright (C) 2020 Infinite Automation Systems Inc. All rights reserved.
 */

package com.infiniteautomation.mango.cache;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

/**
 * @author Jared Wiltshire
 */
public class LRUMap<K, V> extends LinkedHashMap<K, V> {
    private final int capacity;

    public LRUMap(int capacity) {
        super(capacity, 0.75f, true);
        this.capacity = capacity;
    }

    @Override
    protected boolean removeEldestEntry(Entry<K, V> eldest) {
        return size() > capacity;
    }
}
