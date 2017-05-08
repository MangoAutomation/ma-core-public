/*
 * Created on 30-Mar-2006
 */
package com.serotonin.cache;

public interface CachedObjectRetriever<K, E> {
    public E retrieve(K key) throws Exception;
}
