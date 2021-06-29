/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.cache;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Jared Wiltshire
 */
public class BidirectionalCacheTest {

    @Test
    public void canPut() {
        BidirectionalCache<Integer, String> cache = new BidirectionalCache<>(1);
        BidirectionalCache<String, Integer> inverse = cache.inverse();
        cache.put(1, "one");
        assertEquals(1, cache.size());
        assertEquals(1, inverse.size());
        assertEquals("one", cache.get(1));
        assertEquals(1, (long) inverse.get("one"));
    }

    @Test
    public void canPutInverse() {
        BidirectionalCache<Integer, String> cache = new BidirectionalCache<>(1);
        BidirectionalCache<String, Integer> inverse = cache.inverse();
        inverse.put("one", 1);
        assertEquals(1, cache.size());
        assertEquals(1, inverse.size());
        assertEquals("one", cache.get(1));
        assertEquals(1, (long) inverse.get("one"));
    }

    @Test
    public void capacity() {
        BidirectionalCache<Integer, String> cache = new BidirectionalCache<>(1);
        BidirectionalCache<String, Integer> inverse = cache.inverse();
        cache.put(1, "one");
        cache.put(2, "two");
        assertEquals(1, cache.size());
        assertEquals(1, inverse.size());
        assertNull(cache.get(1));
        assertEquals("two", cache.get(2));
        assertNull(inverse.get("one"));
        assertEquals(2, (long) inverse.get("two"));
    }

    @Test
    public void capacityInverse() {
        BidirectionalCache<Integer, String> cache = new BidirectionalCache<>(1);
        BidirectionalCache<String, Integer> inverse = cache.inverse();
        inverse.put("one", 1);
        inverse.put("two", 2);
        assertEquals(1, cache.size());
        assertEquals(1, inverse.size());
        assertNull(cache.get(1));
        assertEquals("two", cache.get(2));
        assertNull(inverse.get("one"));
        assertEquals(2, (long) inverse.get("two"));
    }

    @Test
    public void insertDuplicate() {
        BidirectionalCache<Integer, String> cache = new BidirectionalCache<>(2);
        BidirectionalCache<String, Integer> inverse = cache.inverse();
        cache.put(1, "one");
        cache.put(1, "one");
        assertEquals(1, cache.size());
        assertEquals(1, inverse.size());
        assertEquals("one", cache.get(1));
        assertEquals(1, (long) inverse.get("one"));
    }

    @Test
    public void insertDuplicateInverse() {
        BidirectionalCache<Integer, String> cache = new BidirectionalCache<>(2);
        BidirectionalCache<String, Integer> inverse = cache.inverse();
        inverse.put("one", 1);
        inverse.put("one", 1);
        assertEquals(1, cache.size());
        assertEquals(1, inverse.size());
        assertEquals("one", cache.get(1));
        assertEquals(1, (long) inverse.get("one"));
    }
}