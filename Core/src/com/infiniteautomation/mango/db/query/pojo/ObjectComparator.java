/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.db.query.pojo;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.Translations;

import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Jared Wiltshire
 */
public class ObjectComparator implements Comparator<Object> {

    private final Translations translations;

    public ObjectComparator(Translations translations) {
        this.translations = translations;
    }

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
            return Double.valueOf(((Number) a).doubleValue()).compareTo(((Number) b).doubleValue());
        }

        return toString(a).compareTo(toString(b));
    }

    private String toString(Object arg) {
        if (this.translations != null && arg instanceof TranslatableMessage) {
            return ((TranslatableMessage) arg).translate(this.translations);
        }
        return arg.toString();
    }

    private static boolean isInteger(Object o) {
        return o instanceof Integer || o instanceof Long || o instanceof Short || o instanceof Byte || o instanceof AtomicInteger || o instanceof AtomicLong;
    }

}