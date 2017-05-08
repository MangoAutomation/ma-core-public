/*
    Copyright (C) 2006-2009 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.cache;

/**
 * @author Matthew Lohbihler
 */
class CachedObject<E> {
    private final long time;
    private final E element;
    
    public CachedObject(long time, E element) {
        this.time = time;
        this.element = element;
    }

    public long getTime() {
        return time;
    }
    public E getElement() {
        return element;
    }
}
