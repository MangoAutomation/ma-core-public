/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
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
