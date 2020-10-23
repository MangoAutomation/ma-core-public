/*
 * Copyright (C) 2020 Infinite Automation Systems Inc. All rights reserved.
 */

package com.infiniteautomation.mango.cache;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jared Wiltshire
 */
public class WeakValueCache<K, V> {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final Map<K, MapValueWeakReference> backingMap;
    private final ReferenceQueue<V> referenceQueue = new ReferenceQueue<>();
    private final ReadWriteLock cacheLock;

    public WeakValueCache() {
        this(new HashMap<>(), new ReentrantReadWriteLock());
    }

    public WeakValueCache(Map<K, MapValueWeakReference> backingMap, ReadWriteLock cacheLock) {
        this.backingMap = backingMap;
        this.cacheLock = cacheLock;
    }

    public V put(K key, V value) {
        cacheLock.writeLock().lock();
        try {
            MapValueWeakReference previous = backingMap.put(key, new MapValueWeakReference(key, value));
            cleanupInternal();
            return previous == null ? null : previous.get();
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    public V remove(K key) {
        cacheLock.writeLock().lock();
        try {
            MapValueWeakReference previous = backingMap.remove(key);
            cleanupInternal();
            return previous == null ? null : previous.get();
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    public V get(K key) {
        cacheLock.readLock().lock();
        try {
            return getInternal(key);
        } finally {
            cacheLock.readLock().unlock();
        }
    }

    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        V value;

        cacheLock.readLock().lock();
        try {
            value = getInternal(key);
        } finally {
            cacheLock.readLock().unlock();
        }

        if (value == null) {
            cacheLock.writeLock().lock();
            try {
                value = getInternal(key);
                if (value == null) {
                    value = mappingFunction.apply(key);
                    if (value != null) {
                        backingMap.put(key, new MapValueWeakReference(key, value));
                    }
                }
                cleanupInternal();
            } finally {
                cacheLock.writeLock().unlock();
            }
        }

        return value;
    }

    private V getInternal(K key) {
        MapValueWeakReference ref = backingMap.get(key);
        return ref == null ? null : ref.get();
    }

    public void forEachValue(Consumer<? super V> consumer) {
        cacheLock.readLock().lock();
        try {
            backingMap.values().forEach(v -> {
                V value = v.get();
                if (value != null) {
                    consumer.accept(value);
                }
            });
        } finally {
            cacheLock.readLock().unlock();
        }
    }

    public void clear() {
        cacheLock.writeLock().lock();
        try {
            backingMap.clear();
            cleanupInternal();
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    public void cleanup() {
        cacheLock.writeLock().lock();
        try {
            cleanupInternal();
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    private void cleanupInternal() {
        try {
            int collectedEntries = 0;
            int removedEntries = 0;

            MapValueWeakReference ref;
            while ((ref = (MapValueWeakReference) referenceQueue.poll()) != null) {
                collectedEntries++;
                if (backingMap.remove(ref.key, ref)) {
                    removedEntries++;
                }
                if (log.isInfoEnabled()) {
                    log.info("Garbage collected {}", ref.key);
                }
            }

            if (log.isInfoEnabled()) {
                log.info("Garbage collected {} entries, removed {} entries, map size {}, keys {}",
                        collectedEntries, removedEntries, backingMap.size(), backingMap.keySet());
            }
        } catch (Exception e) {
            log.error("Error cleaning up cache", e);
        }
    }

    public int size() {
        cacheLock.readLock().lock();
        try {
            return backingMap.size();
        } finally {
            cacheLock.readLock().unlock();
        }
    }

    private class MapValueWeakReference extends WeakReference<V> {
        private final K key;

        public MapValueWeakReference(K key, V referent) {
            super(referent, referenceQueue);
            this.key = key;
        }
    }
}
