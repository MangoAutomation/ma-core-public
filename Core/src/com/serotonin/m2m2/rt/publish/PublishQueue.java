/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.publish;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;

/**
 * 
 * @author Matthew Lohbihler
 */
public class PublishQueue<T extends PublishedPointVO, V> {
    private static final Logger LOG = LoggerFactory.getLogger(PublishQueue.class);
    private static final long SIZE_CHECK_DELAY = 5000;

    protected final ConcurrentLinkedQueue<PublishQueueEntry<T, V>> queue = new ConcurrentLinkedQueue<PublishQueueEntry<T, V>>();
    private final PublisherRT<T> owner;
    private final int warningSize;
    private final int dewarningSize;
    private final int discardSize;
    private boolean warningActive = false;
    private long lastSizeCheck;

    public PublishQueue(PublisherRT<T> owner, int warningSize, int discardSize) {
        this.owner = owner;
        this.warningSize = warningSize;
        this.dewarningSize = (int) (warningSize * 0.9); // Deactivate the size warning at 90% of the warning size.
        this.discardSize = discardSize;
    }

    public void add(T vo, V pvt) {
        queue.add(new PublishQueueEntry<T, V>(vo, pvt));
        sizeCheck();
    }

    public void add(T vo, List<V> pvts) {
        for (V pvt : pvts)
            queue.add(new PublishQueueEntry<T, V>(vo, pvt));
        sizeCheck();
    }

    public PublishQueueEntry<T,V> next() {
        return queue.peek();
    }

    public List<PublishQueueEntry<T,V>> get(int max) {
        if (queue.isEmpty())
            return null;

        Iterator<PublishQueueEntry<T,V>> iter = queue.iterator();
        List<PublishQueueEntry<T,V>> result = new ArrayList<PublishQueueEntry<T,V>>(max);
        while (iter.hasNext() && result.size() < max)
            result.add(iter.next());

        return result;
    }

    public void remove(PublishQueueEntry<T,V> e) {
        queue.remove(e);
        sizeCheck();
    }

    public void removeAll(List<PublishQueueEntry<T,V>> list) {
        queue.removeAll(list);
        sizeCheck();
    }
    
    public void removeAll() {
    	queue.clear();
    }

    public int getSize() {
        return queue.size();
    }

    private void sizeCheck() {
        long now = Common.timer.currentTimeMillis();
        if (lastSizeCheck + SIZE_CHECK_DELAY < now) {
            lastSizeCheck = now;
            int size = queue.size();

            synchronized (owner) {
                if (size > discardSize) {
                	try {
                		for (int i = discardSize; i < size; i++)
                			queue.remove();
                	} catch(NoSuchElementException e) {
                		//Queue is emptied, nothing to do
                	}
                	
                    LOG.warn("Publisher queue " + owner.getVo().getName() + " discarded " + (size - discardSize)
                            + " entries");
                }

                if (warningActive) {
                    if (size <= dewarningSize) {
                        owner.deactivateQueueSizeWarningEvent();
                        warningActive = false;
                    }
                }
                else {
                    if (size > warningSize) {
                        owner.fireQueueSizeWarningEvent();
                        warningActive = true;
                    }
                }
            }
        }
    }
}
