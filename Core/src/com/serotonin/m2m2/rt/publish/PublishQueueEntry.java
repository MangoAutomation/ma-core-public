/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.publish;

import com.serotonin.m2m2.vo.publish.PublishedPointVO;

/**
 * @author Matthew Lohbihler
 */
public class PublishQueueEntry<T extends PublishedPointVO, V> {
    private final T vo;
    private final V value;

    public PublishQueueEntry(T vo, V pvt) {
        this.vo = vo;
        this.value = pvt;
    }

    public T getVo() {
        return vo;
    }

    public V getValue() {
        return value;
    }
}
