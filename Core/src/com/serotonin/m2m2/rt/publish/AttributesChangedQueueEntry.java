/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.publish;

import java.util.Map;

import com.serotonin.m2m2.vo.publish.PublishedPointVO;

/**
 * Data for attributes change on a published point
 * @author Terry Packer
 */
public class AttributesChangedQueueEntry<T extends PublishedPointVO> {

    final PublishedPointRT<T> rt;
    final Map<String, Object> attributes;
    
    /**
     */
    public AttributesChangedQueueEntry(PublishedPointRT<T> rt, Map<String, Object> attributes) {
        this.rt = rt;
        this.attributes = attributes;
    }

    public PublishedPointRT<T> getRt() {
        return rt;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    
}
