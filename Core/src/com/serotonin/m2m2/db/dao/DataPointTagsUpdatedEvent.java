/*
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.db.dao;

import org.springframework.context.ApplicationEvent;

import com.infiniteautomation.mango.spring.eventMulticaster.PropagatingEvent;
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