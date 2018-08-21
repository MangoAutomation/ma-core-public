/*
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.db.dao;

import java.util.Objects;

import org.springframework.context.ApplicationEvent;
import org.springframework.core.ResolvableType;
import org.springframework.core.ResolvableTypeProvider;

import com.infiniteautomation.mango.spring.eventMulticaster.PropagatingEvent;
import com.serotonin.m2m2.vo.AbstractBasicVO;

/**
 * @author Jared Wiltshire
 */
public class DaoEvent<T extends AbstractBasicVO> extends ApplicationEvent implements ResolvableTypeProvider, PropagatingEvent {
    private static final long serialVersionUID = 1L;

    private final DaoEventType type;
    private final T vo;
    private final String initiatorId;
    private final String originalXid;

    /**
     * @param source
     */
    public DaoEvent(AbstractBasicDao<T> source, DaoEventType type, T vo, String initiatorId, String originalXid) {
        super(source);
        this.type = Objects.requireNonNull(type);
        this.vo = Objects.requireNonNull(vo);
        this.initiatorId = initiatorId;
        this.originalXid = originalXid;
    }

    public DaoEventType getType() {
        return type;
    }

    public T getVo() {
        return vo;
    }

    public String getInitiatorId() {
        return initiatorId;
    }

    public String getOriginalXid() {
        return originalXid;
    }

    /**
     * Enables Spring to determine the full type of this event (including generics) and only publish
     * it to listeners of the correct type.
     */
    @Override
    public ResolvableType getResolvableType() {
        return ResolvableType.forClassWithGenerics(this.getClass(), this.vo.getClass());
    }

}
