/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.event.handlers;

import com.serotonin.m2m2.vo.event.AbstractEventHandlerVO;


abstract public class EventHandlerRT<T extends AbstractEventHandlerVO> implements EventHandlerInterface {

    protected T vo;

    public EventHandlerRT(T vo){
        this.vo = vo;
    }

}
