/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.db;

/**
 * Callback for a row mapped to an Object
 *
 *
 */
@FunctionalInterface
public interface MappedRowCallback<T> {
    void row(T item, int index);
}
