/*
 * Copyright (C) 2023 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.rt.publish;

import java.util.Collection;
import java.util.List;

import org.springframework.lang.Nullable;

import com.serotonin.m2m2.vo.publish.PublishedPointVO;

/**
 * @author Jared Wiltshire
 */
public interface PublishQueue<T extends PublishedPointVO, V> {
    /**
     * Add an entry to the tail of the queue.
     *
     * @param vo the published point to which this entry belongs
     * @param item the point value/attribute for the published point
     */
    void add(T vo, V item);

    /**
     * Add entries to the tail of the queue.
     *
     * @param vo the published point to which these entries belong
     * @param items the point values/attributes for the published point
     */
    void add(T vo, Collection<V> items);

    /**
     * Retrieve a single entry from the head of the queue without removing it (peek).
     *
     * @return entry from head of queue, or null if the queue is empty.
     */
    @Nullable
    PublishQueueEntry<T, V> next();

    /**
     * Retrieves multiple entries from the head of the queue without removing them (peek).
     *
     * @param max maximum number of entries to retrieve
     * @throws IllegalArgumentException if max is less than 0
     * @return list of entries from head of queue
     */
    List<PublishQueueEntry<T, V>> get(int max);

    /**
     * Removes a single entry from the queue (by identity).
     *
     * @param entry entry to remove
     */
    void remove(PublishQueueEntry<T, V> entry);

    /**
     * Removes all the specified entries from the queue (by identity).
     *
     * @param entries entries to be removed
     */
    void removeAll(Collection<PublishQueueEntry<T, V>> entries);

    /**
     * Clears the queue, removing all entries.
     */
    void removeAll();

    /**
     * @return number of items stored in the queue
     */
    int getSize();

    /**
     * Called when the publisher which owns this queue is terminated.
     */
    void terminate();
}
