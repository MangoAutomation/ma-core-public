/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.publish;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;

import com.infiniteautomation.mango.monitor.ValueMonitor;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;

/**
 * 
 * @author Matthew Lohbihler
 */
public class PublishQueueImpl<T extends PublishedPointVO, V> implements PublishQueue<T, V> {
    private static final Logger LOG = LoggerFactory.getLogger(PublishQueueImpl.class);
    private static final long SIZE_CHECK_DELAY = 5000;

    //Metrics
    public static final String QUEUE_SIZE_MONITOR_ID = "com.serotonin.m2m2.rt.publish.QUEUE_SIZE_MONITOR_";

    //Monitors
    final ValueMonitor<Integer> queueSizeMonitor;

    protected final ConcurrentLinkedQueue<PublishQueueEntry<T, V>> queue = new ConcurrentLinkedQueue<>();
    private final PublisherRT<?, T, ?> owner;
    private final int warningSize;
    private final int dewarningSize;
    private final int discardSize;
    private boolean warningActive = false;
    private long lastSizeCheck;

    public PublishQueueImpl(PublisherRT<?, T, ?> owner, int warningSize, int discardSize) {
        this.owner = owner;
        this.warningSize = warningSize;
        this.dewarningSize = (int) (warningSize * 0.9); // Deactivate the size warning at 90% of the warning size.
        this.discardSize = discardSize;
        this.queueSizeMonitor = Common.MONITORED_VALUES.<Integer>create(QUEUE_SIZE_MONITOR_ID + this.owner.getVo().getXid())
                .name(new TranslatableMessage("publisher.monitor.QUEUE_SIZE_MONITOR_ID", this.owner.getVo().getName())).build();
    }

    @Override
    public void add(T vo, V item) {
        queue.add(new PublishQueueEntry<>(vo, item));
        sizeCheck();
    }

    @Override
    public void add(T vo, Collection<V> items) {
        for (V pvt : items)
            queue.add(new PublishQueueEntry<>(vo, pvt));
        sizeCheck();
    }

    @Override
    public @Nullable PublishQueueEntry<T,V> next() {
        return queue.peek();
    }

    @Override
    public List<PublishQueueEntry<T,V>> get(int max) {
        if (max < 0) throw new IllegalArgumentException();
        if (max == 0 || queue.isEmpty())
            return List.of();

        Iterator<PublishQueueEntry<T,V>> iter = queue.iterator();
        List<PublishQueueEntry<T,V>> result = new ArrayList<>(max);
        while (iter.hasNext() && result.size() < max)
            result.add(iter.next());

        return result;
    }

    @Override
    public void remove(PublishQueueEntry<T, V> entry) {
        queue.remove(entry);
        sizeCheck();
    }

    @Override
    public void removeAll(Collection<PublishQueueEntry<T, V>> entries) {
        queue.removeAll(entries);
        sizeCheck();
    }
    
    @Override
    public void removeAll() {
    	queue.clear();
    }

    @Override
    public int getSize() {
        return queue.size();
    }

    private void sizeCheck() {
        long now = Common.timer.currentTimeMillis();
        if (lastSizeCheck + SIZE_CHECK_DELAY < now) {
            lastSizeCheck = now;
            int size = queue.size();
            queueSizeMonitor.setValue(size);
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

    @Override
    public void terminate() {
        Common.MONITORED_VALUES.remove(this.queueSizeMonitor.getId());
        if(!queue.isEmpty()){
            LOG.debug("Publisher " + owner.readableIdentifier() + " terminated with a non-empty queue.");
        }
    }
}
