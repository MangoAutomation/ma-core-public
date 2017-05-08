package com.serotonin.util;

import java.util.Enumeration;
import java.util.Iterator;

public class EnumerationIterator<T> implements Iterable<T> {
    final Enumeration<T> enumeration;

    public EnumerationIterator(Enumeration<T> enumeration) {
        this.enumeration = enumeration;
    }

    public Iterator<T> iterator() {
        return new Iter();
    }

    class Iter implements Iterator<T> {
        public boolean hasNext() {
            return enumeration.hasMoreElements();
        }

        public T next() {
            return enumeration.nextElement();
        }

        public void remove() {
            throw new RuntimeException("not implemented");
        }
    }
}
