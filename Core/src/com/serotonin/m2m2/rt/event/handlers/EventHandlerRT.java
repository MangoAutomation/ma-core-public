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

}
