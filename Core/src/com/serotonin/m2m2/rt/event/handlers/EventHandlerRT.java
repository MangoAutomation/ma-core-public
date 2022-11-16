/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.event.handlers;

import com.serotonin.m2m2.vo.event.AbstractEventHandlerVO;


abstract public class EventHandlerRT<T extends AbstractEventHandlerVO> implements EventHandlerInterface {

    protected T vo;

    public EventHandlerRT(T vo){
        this.vo = vo;
    }

    public T getVo() {
        return vo;
    }

    @SuppressWarnings("unchecked")
    public void setVo(AbstractEventHandlerVO vo) {
        this.vo = (T) vo;
    }

    public void terminate() {}

}
