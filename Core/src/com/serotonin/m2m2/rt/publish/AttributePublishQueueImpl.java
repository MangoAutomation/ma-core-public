/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.publish;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import com.serotonin.m2m2.vo.publish.PublishedPointVO;

/**
 * 
 * This queue is thread safe and backed by an ordered map of attributes to PublishedPointVO
 * 
 * @author Terry Packer
 *
 */
public class AttributePublishQueueImpl<T extends PublishedPointVO> implements AttributePublishQueue<T> {

    //Create a fair lock in effort to ensure ordering is kept
    protected final ReentrantLock lock = new ReentrantLock(true);
    protected final LinkedHashMap<Integer, PublishQueueEntry<T, Map<String, Object>>> attributesToBePublished = new LinkedHashMap<>();

    @Override
    public void add(T vo, Map<String, Object> value) {
        lock.lock();
        try {
            attributesToBePublished.put(vo.getDataPointId(), new PublishQueueEntry<>(vo, value));
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public void attributePublishFailed(PublishQueueEntry<T, Map<String, Object>> entry) {
        lock.lock();
        try {
            attributesToBePublished.putIfAbsent(entry.getVo().getDataPointId(), entry);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public PublishQueueEntry<T,Map<String,Object>> next() {
        lock.lock();
        try {
            Iterator<Integer> it = attributesToBePublished.keySet().iterator();
            if (it.hasNext()) {
                return attributesToBePublished.remove(it.next());
            } else {
                return null;
            }
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public void clear() {
        lock.lock();
        try {
            attributesToBePublished.clear();
        } finally {
            lock.unlock();
        }
    }
}
