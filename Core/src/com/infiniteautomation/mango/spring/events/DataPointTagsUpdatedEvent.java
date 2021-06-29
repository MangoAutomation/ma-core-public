/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.spring.events;

import org.springframework.context.ApplicationEvent;

import com.infiniteautomation.mango.spring.eventMulticaster.PropagatingEvent;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.vo.DataPointVO;

public class DataPointTagsUpdatedEvent extends ApplicationEvent implements PropagatingEvent {
    private static final long serialVersionUID = 1L;
    private final DataPointVO vo;

    public DataPointTagsUpdatedEvent(DataPointDao source, DataPointVO vo) {
        super(source);
        this.vo = vo;
    }

    public DataPointVO getVo() {
        return vo;
    }
}