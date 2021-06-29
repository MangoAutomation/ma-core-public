/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.cache;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @author Jared Wiltshire
 */
public interface Cache<K, V> {
    V get(K key);

    int size();

    V put(K key, V value);

    V remove(K key);

    V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction);

    void forEach(BiConsumer<? super K, ? super V> action);

    void clear();

    default boolean isEmpty() {
        return size() == 0;
    }

    boolean containsKey(K key);

    V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction);

    V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction);
}
