/*
 * Copyright (C) 2023 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.rt.publish;

import java.util.Map;

import com.serotonin.m2m2.vo.publish.PublishedPointVO;

/**
 * @author Terry Packer
 * @author Jared Wiltshire
 */
public interface AttributePublishQueue<T extends PublishedPointVO> {
    /**
     * Add attributes to be published.  There is only 1 entry for attributes per published point and the order is
     * insertion based.
     */
    void add(T vo, Map<String, Object> value);

    /**
     * Re-add the attributes to the queue if an entry for this vo does not already exist.
     * This is used when the attributes didn't get published and potentially an attribute update
     * happened while they were out of this structure.
     */
    void attributePublishFailed(PublishQueueEntry<T, Map<String, Object>> entry);

    /**
     * Remove the next item from the queue
     */
    PublishQueueEntry<T, Map<String, Object>> next();

    /**
     * Empty the queue entirely
     */
    void clear();
}
