/**
 * Copyright (C) 2018  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.rt.publish;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;

/**
 * TODO Mango 3.6 stop extending PublishQueue and fix PublisherRT.  This structure should be 
 * constrained on size to the number of published points.
 * 
 * The next question is if we really want to order this data structure, the queue was previously
 * ordered on insertion but this map is not.  
 * 
 * @author Terry Packer
 *
 */
public class AttributePublishQueue<T extends PublishedPointVO> extends PublishQueue<T, Map<String,Object>> {

    //TODO keep keys as integer, values as PublishQueueEntry
    protected ConcurrentHashMap<Integer, PublishQueueEntry<T, Map<String, Object>>> attributesToBePublished;
    
    /**
     * @param owner
     * @param warningSize
     * @param discardSize
     */
    public AttributePublishQueue(PublisherRT<T> owner, int warningSize, int discardSize) {
        super(owner, warningSize, discardSize);
        this.attributesToBePublished = new ConcurrentHashMap<>();
    }

    public void add(T vo, Map<String,Object> value) {
        attributesToBePublished.put(vo.getDataPointId(), new PublishQueueEntry<T, Map<String, Object>>(vo, value));
    }

    public void add(T vo, List<Map<String,Object>> value) {
        throw new ShouldNeverHappenException("Unimplemented");
    }

    public PublishQueueEntry<T,Map<String,Object>> next() {
        Iterator<Entry<Integer ,PublishQueueEntry<T, Map<String, Object>>>> iter = attributesToBePublished.entrySet().iterator();
        if(!iter.hasNext())
            return null;
        Entry<Integer, PublishQueueEntry<T, Map<String, Object>>> entry = iter.next();
        attributesToBePublished.compute(entry.getKey(), (k, v) -> {
            if (v == entry.getValue())
                return null;
            return v;
        });
        return entry.getValue();
    }

    public List<PublishQueueEntry<T,Map<String,Object>>> get(int max) {
        throw new ShouldNeverHappenException("Unimplemented");
    }

    public void remove(PublishQueueEntry<T,Map<String,Object>> e) {
        //No-Op
        return;
    }

    public void removeAll(List<PublishQueueEntry<T,Map<String,Object>>> list) {
        throw new ShouldNeverHappenException("Unimplemented");
    }
    
    public void removeAll() {
        attributesToBePublished.clear();
    }

    public int getSize() {
        return attributesToBePublished.size();
    }
    
    
}
