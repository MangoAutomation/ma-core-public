/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.cache;


import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;


/**
 * @author Jared Wiltshire
 */
public class BidirectionalCache<K, V> implements Cache<K, V> {

    private final Map<K, V> forward;
    private final Map<V, K> reverse;
    private final ReadWriteLock lock;

    public BidirectionalCache(int capacity) {
        this(new LRUMapWithReverse<>(capacity));
    }

    private BidirectionalCache(LRUMapWithReverse<K, V> forward) {
        this(forward, forward.reverse, new ReentrantReadWriteLock());
    }

    private BidirectionalCache(Map<K, V> forward, Map<V, K> reverse, ReadWriteLock lock) {
        this.forward = forward;
        this.reverse = reverse;
        this.lock = lock;
    }

    public BidirectionalCache<V, K> inverse() {
        return new BidirectionalCache<>(this.reverse, this.forward, this.lock);
    }

    @Override
    public V get(K key) {
        lock.readLock().lock();
        try {
            return forward.get(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int size() {
        lock.readLock().lock();
        try {
            return forward.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public V put(K key, V value) {
        lock.writeLock().lock();
        try {
            reverse.put(value, key);
            return forward.put(key, value);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public V remove(K key) {
        lock.writeLock().lock();
        try {
            V removed = forward.remove(key);
            if (removed != null) {
                reverse.remove(removed);
            }
            return removed;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        Objects.requireNonNull(action);
        lock.readLock().lock();
        try {
            for (Entry<K, V> entry : forward.entrySet()) {
                action.accept(entry.getKey(), entry.getValue());
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            forward.clear();
            reverse.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean containsKey(K key) {
        lock.readLock().lock();
        try {
            return forward.containsKey(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        lock.readLock().lock();
        try {
            V value = forward.get(key);
            if (value != null) {
                return value;
            }
        } finally {
            lock.readLock().unlock();
        }

        lock.writeLock().lock();
        try {
            V oldValue = forward.get(key);
            if (oldValue != null) {
                return oldValue;
            }
            V newValue = mappingFunction.apply(key);
            if (newValue != null) {
                reverse.put(newValue, key);
                forward.put(key, newValue);
            }
            return newValue;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        lock.readLock().lock();
        try {
            V value = forward.get(key);
            if (value == null) {
                return null;
            }
        } finally {
            lock.readLock().unlock();
        }

        lock.writeLock().lock();
        try {
            V oldValue = forward.get(key);
            if (oldValue == null) {
                return null;
            }
            V newValue = remappingFunction.apply(key, oldValue);
            if (newValue == null) {
                reverse.remove(oldValue);
                forward.remove(key);
            } else {
                reverse.put(newValue, key);
                forward.put(key, newValue);
            }
            return newValue;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        lock.writeLock().lock();
        try {
            V oldValue = forward.get(key);
            V newValue = remappingFunction.apply(key, oldValue);
            if (newValue == null) {
                if (oldValue != null) {
                    reverse.remove(oldValue);
                    forward.remove(key);
                }
            } else {
                reverse.put(newValue, key);
                forward.put(key, newValue);
            }
            return newValue;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private static class LRUMapWithReverse<K1, V1> extends LRUMap<K1, V1> {
        private final Map<V1, K1> reverse;

        public LRUMapWithReverse(int capacity) {
            super(capacity);
            this.reverse = new HashMap<>(capacity);
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K1, V1> eldest) {
            boolean remove = super.removeEldestEntry(eldest);
            if (remove) {
                reverse.remove(eldest.getValue());
            }
            return remove;
        }
    }
}
