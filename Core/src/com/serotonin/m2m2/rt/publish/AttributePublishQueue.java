/**
 * Copyright (C) 2018  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.rt.publish;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.serotonin.m2m2.vo.publish.PublishedPointVO;

/**
 * 
 * This queue is thread safe and backed by an ordered map of attributes to PublishedPointVO
 * 
 * @author Terry Packer
 *
 */
public class AttributePublishQueue<T extends PublishedPointVO> {

    protected final ReadWriteLock lock = new ReentrantReadWriteLock();
    protected final LinkedHashMap<Integer, PublishQueueEntry<T, Map<String, Object>>> attributesToBePublished;
    
    public AttributePublishQueue(int initialSize) {
        this.attributesToBePublished = new LinkedHashMap<>(initialSize);
    }

    public void add(T vo, Map<String,Object> value) {
        lock.writeLock().lock();
        try {
            attributesToBePublished.put(vo.getDataPointId(), new PublishQueueEntry<T, Map<String, Object>>(vo, value));
        }finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Re-add the attributes to the queue if an entry for this vo does not already exist.  
     *  This is used when the attributes didn't get published and potentially an attribute update
     *  happened while they were out of this structure.
     *  
     * @param entry
     */
    public void attributePublishFailed(PublishQueueEntry<T,Map<String,Object>> entry) {
        lock.writeLock().lock();
        try {
            attributesToBePublished.computeIfAbsent(entry.getVo().getDataPointId(), k -> entry);
        }finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Remove the next item from the queue
     * @return
     */
    public PublishQueueEntry<T,Map<String,Object>> next() {
        lock.writeLock().lock();
        try {
            Iterator<Integer> it = attributesToBePublished.keySet().iterator();
            if(it.hasNext()) {
                return attributesToBePublished.remove(it.next());
            }else
                return null;
        }finally {
            lock.writeLock().unlock();
        }
    }
    
    public void removeAll() {
        lock.writeLock().lock();
        try {
            attributesToBePublished.clear();
        }finally {
            lock.writeLock().unlock();
        }
    }

    public int getSize() {
        lock.readLock().lock();
        try {
            return attributesToBePublished.size();
        }finally {
            lock.readLock().unlock();
        }
    }
    
}
