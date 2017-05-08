package com.serotonin.db;

public interface MappedRowCallback<T> {
    void row(T item, int index);
}
