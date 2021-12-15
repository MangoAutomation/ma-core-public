/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 * @Author Terry Packer
 *
 */

package com.infiniteautomation.mango.spring.events;

import org.springframework.context.ApplicationEvent;
import org.springframework.core.ResolvableType;
import org.springframework.core.ResolvableTypeProvider;

import com.infiniteautomation.mango.spring.eventMulticaster.PropagatingEvent;
import com.serotonin.m2m2.db.dao.AbstractBasicDao;
import com.serotonin.m2m2.vo.AbstractActionVO;
import com.serotonin.util.ILifecycleState;

/**
 * Container for lifecycle DaoEvents
 * @param <T> action VO type
 */
public class StateChangeEvent<T extends AbstractActionVO> extends ApplicationEvent implements ResolvableTypeProvider, PropagatingEvent {

    public static final String STATE_CHANGE = "stateChange";
    private final T vo;
    private final ILifecycleState state;

    public StateChangeEvent(AbstractBasicDao<T, ?, ?> source, ILifecycleState state, T vo) {
        super(source);
        this.vo = vo;
        this.state = state;
    }

    public ILifecycleState getState() {
        return state;
    }

    public T getVo() {
        return vo;
    }

    /**
     * Enables Spring to determine the full type of this event (including generics) and only publish
     * it to listeners of the correct type.
     */
    @Override
    public ResolvableType getResolvableType() {
        return ResolvableType.forClassWithGenerics(this.getClass(), ResolvableType.forClass(this.vo.getClass()));
    }

    @Override
    public String toString() {
        return "StateChangeEvent [state=" + state + ", vo=" + vo + "]";
    }

}
