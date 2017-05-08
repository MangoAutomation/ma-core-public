/*
 * Created on 30-Mar-2006
 */
package com.serotonin.cache;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class DataCache<K, E> {
    private final Map<K, CachedObject<E>> cache = new HashMap<K, CachedObject<E>>();
    private long cacheUpdatePeriod = 3600000; // 1 hour
    private CachedObjectRetriever<K, E> defaultRetriever = null;

    /**
     * @return Returns the cacheUpdatePeriod.
     */
    public long getCacheUpdatePeriod() {
        return cacheUpdatePeriod;
    }

    /**
     * @param cacheUpdatePeriod
     *            The cacheUpdatePeriod to set.
     */
    public void setCacheUpdatePeriod(long cacheUpdatePeriod) {
        this.cacheUpdatePeriod = cacheUpdatePeriod;
    }

    public CachedObjectRetriever<K, E> getDefaultRetriever() {
        return defaultRetriever;
    }

    public void setDefaultRetriever(CachedObjectRetriever<K, E> defaultRetriever) {
        this.defaultRetriever = defaultRetriever;
    }

    public E getCachedObject(K key) throws Exception {
        return getCachedObject(key, defaultRetriever);
    }

    public E getCachedObject(K key, CachedObjectRetriever<K, E> retriever) throws Exception {
        long now = System.currentTimeMillis();

        // Check if the object is in the cache.
        CachedObject<E> co = cache.get(key);
        if (co == null || co.getTime() + cacheUpdatePeriod < now) {
            // The object isn't in the cache, so get a lock on the cache.
            synchronized (cache) {
                // Since another thread may have put the object in the cache while we were
                // waiting for the lock, do one more check.
                co = cache.get(key);
                if (co == null || co.getTime() + cacheUpdatePeriod < now) {
                    // Ok, the object is definitely not in the cache, and no other thread
                    // can get it in there because we have the lock, so let's retrieve it
                    // and put it in.
                    E obj = retriever.retrieve(key);
                    co = new CachedObject<E>(now, obj);
                    cache.put(key, co);
                }
            }
        }

        // Return the object.
        return co.getElement();
    }

    public void removeCachedObject(K key) {
        synchronized (cache) {
            cache.remove(key);
        }
    }

    public void checkForExpiredEntries() {
        long now = System.currentTimeMillis();

        synchronized (cache) {
            Iterator<Map.Entry<K, CachedObject<E>>> iter = cache.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<K, CachedObject<E>> entry = iter.next();
                if (entry.getValue().getTime() + cacheUpdatePeriod < now)
                    iter.remove();
            }
        }
    }
}
