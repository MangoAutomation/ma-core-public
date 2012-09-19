/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.publish;

import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;

/**
 * @author Matthew Lohbihler
 */
public class PublishQueueEntry<T extends PublishedPointVO> {
    private final T vo;
    private final PointValueTime pvt;

    public PublishQueueEntry(T vo, PointValueTime pvt) {
        this.vo = vo;
        this.pvt = pvt;
    }

    public T getVo() {
        return vo;
    }

    public PointValueTime getPvt() {
        return pvt;
    }
}
