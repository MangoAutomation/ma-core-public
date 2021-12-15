/*
    Copyright (C) 2006-2007 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.ObjectUtils;

/**
 * @author Matthew Lohbihler
 */
public class CollectionUtils {
    public static boolean containsDuplicates(List<?> list) {
        if (list == null)
            return false;

        for (int i = 0; i < list.size() - 1; i++) {
            Object o = list.get(i);
            for (int j = i + 1; j < list.size(); j++) {
                if (ObjectUtils.equals(o, list.get(j)))
                    return true;
            }
        }
        return false;
    }

    public static String implode(List<?> values, String delimeter) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Object o : values) {
            if (first)
                first = false;
            else
                sb.append(delimeter);
            sb.append(o);
        }
        return sb.toString();
    }

    /**
     * Compares to lists for equality, regardless of current sort order.
     *
     */
    public static boolean equal(List<?> list1, List<?> list2) {
        if (list1 == null && list2 == null)
            return true;
        if (list1 == null || list2 == null)
            return false;

        if (list1.size() != list2.size())
            return false;

        List<?> copy = new ArrayList<Object>(list2);

        int i, j;
        Object o1, o2;
        boolean found;
        for (i = list1.size() - 1; i >= 0; i--) {
            o1 = list1.get(i);

            found = false;
            for (j = copy.size() - 1; j >= 0; j--) {
                o2 = copy.get(j);
                if (ObjectUtils.equals(o1, o2)) {
                    found = true;
                    break;
                }
            }

            if (found)
                copy.remove(j);
            else
                return false;
        }

        return true;
    }

    /**
     * Removes all common elements from the
     * 
     * @param <T>
     *            the element type of the lists
     * @param all
     *            the list of elements that should be removed
     * @param from
     *            the list from which elements should be removed
     * @return the list of elements in from which remain after removed the elements in all.
     */
    public static <T> List<T> subtract(Collection<T> all, Collection<T> from) {
        if (from == null)
            return Collections.emptyList();

        List<T> remainder = new ArrayList<T>();
        for (T t : from) {
            if (all == null || !all.contains(t))
                remainder.add(t);
        }
        return remainder;
    }

    /**
     * Returns a list of entries that are duplicated.
     */
    public static <T> Set<T> duplicates(List<T> list) {
        if (list == null)
            return Collections.emptySet();

        Set<T> result = new HashSet<T>();
        for (int i = 0; i < list.size() - 1; i++) {
            T o = list.get(i);
            for (int j = i + 1; j < list.size(); j++) {
                if (ObjectUtils.equals(o, list.get(j))) {
                    result.add(o);
                    break;
                }
            }
        }
        return result;
    }

    //    public static <T> List<T> toList(T[] arr) {
    //        List<T> list = new ArrayList<T>(elements.length);
    //        for (Object e : elements)
    //            list.add((T) e);
    //        return list;
    //    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> toList(T... elements) {
        List<T> list = new ArrayList<T>(elements.length);
        for (Object e : elements)
            list.add((T) e);
        return list;
    }
}
