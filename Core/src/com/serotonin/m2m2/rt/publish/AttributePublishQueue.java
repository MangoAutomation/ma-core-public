/**
 * Copyright (C) 2018  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.rt.publish;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.serotonin.m2m2.vo.publish.PublishedPointVO;

/**
 * 
 * This queue is thread safe and backed by an ordered map of attributes to PublishedPointVO
 * 
 * @author Terry Packer
 *
 */
public class AttributePublishQueue<T extends PublishedPointVO> {

    protected final Object LOCK = new Object();
    protected final LinkedHashMap<Integer, PublishQueueEntry<T, Map<String, Object>>> attributesToBePublished;
    
    public AttributePublishQueue(int initialSize) {
        this.attributesToBePublished = new LinkedHashMap<>(initialSize);
    }

    /**
     * Add attributes to be published.  There is only 1 entry for attributes per published point and the order is 
     *   insertion based.
     * @param vo
     * @param value
     */
    public void add(T vo, Map<String,Object> value) {
        synchronized(LOCK) {
            attributesToBePublished.put(vo.getDataPointId(), new PublishQueueEntry<T, Map<String, Object>>(vo, value));
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
        synchronized(LOCK) {
            attributesToBePublished.computeIfAbsent(entry.getVo().getDataPointId(), k -> entry);
        }
    }

    /**
     * Remove the next item from the queue
     * @return
     */
    public PublishQueueEntry<T,Map<String,Object>> next() {
        synchronized(LOCK) {
            Iterator<Integer> it = attributesToBePublished.keySet().iterator();
            if(it.hasNext()) {
                return attributesToBePublished.remove(it.next());
            }else
                return null;
        }
    }
    
    /**
     * Empty the queue entirely
     */
    public void clear() {
        synchronized(LOCK) {
            attributesToBePublished.clear();
        }
    }
}
