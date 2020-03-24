/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.db.query.pojo;

import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Jared Wiltshire
 */
public class ObjectComparator implements Comparator<Object> {

    public static final Comparator<Object> INSTANCE = new ObjectComparator();

    private ObjectComparator() {}

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public int compare(Object a, Object b) {
        if (a == b) return 0;
        if (a == null) return -1;
        if (b == null) return 1;

        if (a.getClass().isInstance(b) && a instanceof Comparable) {
            return ((Comparable) a).compareTo(b);
        } else if (b.getClass().isInstance(a) && b instanceof Comparable) {
            return -((Comparable) b).compareTo(a);
        }

        if (a instanceof Number && b instanceof Number) {
            if (isInteger(a) && isInteger(b)) {
                return Long.valueOf(((Number) a).longValue()).compareTo(((Number) b).longValue());
            }
            return Double.valueOf(((Number) a).doubleValue()).compareTo(((Double) b).doubleValue());
        }

        return a.toString().compareTo(b.toString());
    }

    private static boolean isInteger(Object o) {
        return o instanceof Integer || o instanceof Long || o instanceof Short || o instanceof Byte || o instanceof AtomicInteger || o instanceof AtomicLong;
    }

}