package com.serotonin.cache;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ThreadSafeCache<T> {
    private final ObjectCreator<T> creator;
    private long purgeDelay;
    private long lastPurge;
    private final Map<Long, T> cache = new ConcurrentHashMap<Long, T>();

    public ThreadSafeCache(ObjectCreator<T> creator) {
        this(creator, 1000 * 60 * 60); // Default to every hour
    }

    public ThreadSafeCache(ObjectCreator<T> creator, long purgeDelay) {
        this.creator = creator;
        this.purgeDelay = purgeDelay;
        lastPurge = System.currentTimeMillis();
    }

    public T getObject() {
        T o = cache.get(Thread.currentThread().getId());
        if (o == null) {
            o = creator.create();
            cache.put(Thread.currentThread().getId(), o);
        }

        if (purgeDelay > 0 && (System.currentTimeMillis() - lastPurge > purgeDelay))
            purge();

        return o;
    }

    private void purge() {
        Set<Long> cacheIds = new HashSet<Long>(cache.keySet());
        for (Thread thread : Thread.getAllStackTraces().keySet())
            cacheIds.remove(thread.getId());

        // Any ids that are left over are from defunct threads.
        for (Long id : cacheIds)
            cache.remove(id);

        lastPurge = System.currentTimeMillis();
    }

    static ThreadSafeCache<Long> testCache;

    public static void main(String[] args) {
        testCache = new ThreadSafeCache<Long>(new ObjectCreator<Long>() {
            public Long create() {
                return Thread.currentThread().getId();
            }
        }, 1000 * 10);

        new TestThread().start();
        new TestThread().start();
        new TestThread().start();
    }

    static class TestThread extends Thread {
        @Override
        public void run() {
            for (int i = 0; i < 3; i++) {
                testCache.getObject();
            }
        }
    }
}
