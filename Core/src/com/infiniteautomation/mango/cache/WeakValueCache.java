/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.cache;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jared Wiltshire
 */
public class WeakValueCache<K, V> implements Cache<K,V> {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final Map<K, V> strongReferences;
    private final Map<K, MapValueWeakReference> weakReferences = new LinkedHashMap<>();
    private final ReferenceQueue<V> referenceQueue = new ReferenceQueue<>();
    private final ReadWriteLock cacheLock;

    public WeakValueCache(int capacity) {
        this(capacity, new ReentrantReadWriteLock());
    }

    public WeakValueCache(int capacity, ReadWriteLock cacheLock) {
        this.strongReferences = new LRUMap<>(capacity);
        this.cacheLock = cacheLock;
    }

    @Override
    public V put(K key, V value) {
        cacheLock.writeLock().lock();
        try {
            try {
                strongReferences.put(key, value);
                MapValueWeakReference previous = weakReferences.put(key, new MapValueWeakReference(key, value));
                return previous == null ? null : previous.get();
            } finally {
                cleanupInternal();
            }
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    @Override
    public V remove(K key) {
        cacheLock.writeLock().lock();
        try {
            try {
                strongReferences.remove(key);
                MapValueWeakReference previous = weakReferences.remove(key);
                return previous == null ? null : previous.get();
            } finally {
                cleanupInternal();
            }
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    @Override
    public V get(K key) {
        cacheLock.readLock().lock();
        try {
            return getInternal(key);
        } finally {
            cacheLock.readLock().unlock();
        }
    }

    private V getInternal(K key) {
        MapValueWeakReference ref = weakReferences.get(key);
        return ref == null ? null : ref.get();
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        cacheLock.readLock().lock();
        try {
            for (Entry<K, MapValueWeakReference> entry : weakReferences.entrySet()) {
                V value = entry.getValue().get();
                if (value != null) {
                    action.accept(entry.getKey(), value);
                }
            }
        } finally {
            cacheLock.readLock().unlock();
        }
    }

    @Override
    public void clear() {
        cacheLock.writeLock().lock();
        try {
            try {
                strongReferences.clear();
                weakReferences.clear();
            } finally {
                cleanupInternal();
            }
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
                if (weakReferences.remove(ref.key, ref)) {
                    removedEntries++;
                }
                if (log.isTraceEnabled()) {
                    log.trace("Garbage collected {}", ref.key);
                }
            }

            if (log.isDebugEnabled()) {
                log.debug("Garbage collected {} entries, removed {} entries, map size {}, keys {}",
                        collectedEntries, removedEntries, weakReferences.size(), weakReferences.keySet());
            }
        } catch (Exception e) {
            log.error("Error cleaning up cache", e);
        }
    }

    @Override
    public int size() {
        cacheLock.readLock().lock();
        try {
            return weakReferences.size();
        } finally {
            cacheLock.readLock().unlock();
        }
    }

    @Override
    public boolean containsKey(K key) {
        return get(key) != null;
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        cacheLock.readLock().lock();
        try {
            V value = getInternal(key);
            if (value != null) {
                return value;
            }
        } finally {
            cacheLock.readLock().unlock();
        }

        cacheLock.writeLock().lock();
        try {
            try {
                V oldValue = getInternal(key);
                if (oldValue != null) {
                    return oldValue;
                }
                V newValue = mappingFunction.apply(key);
                if (newValue != null) {
                    strongReferences.put(key, newValue);
                    weakReferences.put(key, new MapValueWeakReference(key, newValue));
                }
                return newValue;
            } finally {
                cleanupInternal();
            }
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    @Override
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        cacheLock.readLock().lock();
        try {
            V value = getInternal(key);
            if (value == null) {
                return null;
            }
        } finally {
            cacheLock.readLock().unlock();
        }

        cacheLock.writeLock().lock();
        try {
            try {
                V oldValue = getInternal(key);
                if (oldValue == null) {
                    return null;
                }
                V newValue = remappingFunction.apply(key, oldValue);
                if (newValue == null) {
                    strongReferences.remove(key);
                    weakReferences.remove(key);
                } else {
                    strongReferences.put(key, newValue);
                    weakReferences.put(key, new MapValueWeakReference(key, newValue));
                }
                return newValue;
            } finally {
                cleanupInternal();
            }
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        cacheLock.writeLock().lock();
        try {
            try {
                V oldValue = getInternal(key);
                V newValue = remappingFunction.apply(key, oldValue);
                if (newValue == null) {
                    if (oldValue != null) {
                        strongReferences.remove(key);
                        weakReferences.remove(key);
                    }
                } else {
                    strongReferences.put(key, newValue);
                    weakReferences.put(key, new MapValueWeakReference(key, newValue));
                }
                return newValue;
            } finally {
                cleanupInternal();
            }
        } finally {
            cacheLock.writeLock().unlock();
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
