package com.serotonin.util;

import java.util.Iterator;

public class IterableIterator<T> implements Iterable<T> {
    private final Iterator<T> iterator;

    public IterableIterator(Iterator<T> iterator) {
        this.iterator = iterator;
    }

    public Iterator<T> iterator() {
        return iterator;
    }
}
