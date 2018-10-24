/**
 * Copyright (C) 2018  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.rt.publish;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

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

    protected ConcurrentHashMap<T, Map<String, Object>> attributesToBePublished;
    
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
        attributesToBePublished.put(vo, value);
    }

    public void add(T vo, List<Map<String,Object>> value) {
        //TODO N/A as this can't happen with attributes
    }

    public PublishQueueEntry<T,Map<String,Object>> next() {
        if(attributesToBePublished.isEmpty())
            return null;
        Iterator<Entry<T,Map<String,Object>>> iter = attributesToBePublished.entrySet().iterator();
        Entry<T,Map<String,Object>> entry = iter.next();
        if(entry != null)
            return new PublishQueueEntry<T, Map<String, Object>>(entry.getKey(), entry.getValue());
        else
            return null;
    }

    public List<PublishQueueEntry<T,Map<String,Object>>> get(int max) {
        if(attributesToBePublished.isEmpty())
            return null;
            
        Iterator<Entry<T,Map<String,Object>>> iter = attributesToBePublished.entrySet().iterator();
        List<PublishQueueEntry<T,Map<String,Object>>> result = new ArrayList<>(max);
        while (iter.hasNext() && result.size() < max) {
            Entry<T,Map<String,Object>> entry = iter.next();
            result.add(new PublishQueueEntry<T, Map<String, Object>>(entry.getKey(), entry.getValue()));
        }

        return result;
    }

    public void remove(PublishQueueEntry<T,Map<String,Object>> e) {
        if(e == null)
            return;
        //TODO Mango 3.6 Make more efficient
        //We cannot rely on the equals/hashcode for T so we must search for data point id
        Iterator<T>  it = attributesToBePublished.keySet().iterator();
        while(it.hasNext()) {
            T next = it.next();
            if(next.getDataPointId() == e.getVo().getDataPointId()) {
                it.remove();
                break;
            }
        }
        attributesToBePublished.remove(e.getVo());
    }

    public void removeAll(List<PublishQueueEntry<T,Map<String,Object>>> list) {
        if(list != null) {
            for(PublishQueueEntry<T,Map<String,Object>> e : list)
                remove(e);
        }
    }
    
    public void removeAll() {
        queue.clear();
    }

    public int getSize() {
        return queue.size();
    }
    
    
}
