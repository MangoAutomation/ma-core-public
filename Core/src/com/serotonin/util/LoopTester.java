/*
    Copyright (C) 2006-2007 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.util;

import java.util.ArrayList;
import java.util.List;

/**
 * This class allows code to test if an infinite loop is occurring. Client code can use it to set indicators in
 * thread local objects so that subsequent calls to the same code can be detected. This is useful for situations
 * where structures of listeners could potentially made cyclical.
 * 
 * Typical user code for this class might look like the following:
 * 
 * if (!LoopCheck.checkAdd(this))
 * return;
 * 
 * try {
 * // Do some work here, including calling listeners.
 * }
 * finally {
 * LoopCheck.remove(this);
 * }
 * 
 * @author Matthew Lohbihler
 */
public class LoopTester {
    private static ThreadLocal<LoopTester> threadLocal = new ThreadLocal<>();

    /**
     * Checks if the object has already been added.
     *
     */
    public static boolean contains(Object o) {
        LoopTester check = threadLocal.get();
        if (check == null)
            return false;
        return check.elements.contains(o);
    }

    /**
     * Adds the object.
     *
     */
    public static void add(Object o) {
        LoopTester check = threadLocal.get();
        if (check == null) {
            check = new LoopTester();
            threadLocal.set(check);
        }
        check.elements.add(o);
    }

    /**
     * A convenience method that combines the contains and add methods. If the object has already been added, it will
     * not be added again, and false will be returned. Otherwise the object will be added and true will be returned.
     * 
     * @param o
     *            the object to conditionally add
     * @return true if the object was added, false if it was already in the list and not added again.
     */
    public static boolean checkAdd(Object o) {
        LoopTester check = threadLocal.get();
        if (check == null) {
            check = new LoopTester();
            threadLocal.set(check);
        }
        else if (check.elements.contains(o))
            return false;

        check.elements.add(o);
        return true;
    }

    public static void remove(Object o) {
        LoopTester check = threadLocal.get();
        if (check == null)
            return;
        check.elements.remove(o);
        if (check.elements.size() == 0)
            threadLocal.remove();
    }

    private final List<Object> elements = new ArrayList<>();
}
