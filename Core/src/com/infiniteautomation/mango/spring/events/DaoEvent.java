/*
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.events;

import java.util.Objects;

import org.springframework.context.ApplicationEvent;
import org.springframework.core.ResolvableType;
import org.springframework.core.ResolvableTypeProvider;

import com.infiniteautomation.mango.spring.eventMulticaster.PropagatingEvent;
import com.serotonin.m2m2.db.dao.AbstractBasicDao;
import com.serotonin.m2m2.vo.AbstractBasicVO;

/**
 * @author Jared Wiltshire
 */
public class DaoEvent<T extends AbstractBasicVO> extends ApplicationEvent implements ResolvableTypeProvider, PropagatingEvent {
    private static final long serialVersionUID = 1L;

    private final DaoEventType type;
    private final T vo;
    private final T originalVo;

    /**
     * Create an event
     * @param source
     * @param type
     * @param vo
     * @param originalXid
     * @param updatedFields
     */
    public DaoEvent(AbstractBasicDao<T,?> source, DaoEventType type, T vo) {
        super(source);
        this.type = Objects.requireNonNull(type);
        this.vo = Objects.requireNonNull(vo);
        this.originalVo = null;

        if (this.type == DaoEventType.UPDATE) {
            throw new IllegalArgumentException("originalVo must be supplied for an UPDATE event");
        }
    }

    /**
     * Create an event, including the previous version of the vo
     * @param source
     * @param type
     * @param vo
     * @param originalXid
     */
    public DaoEvent(AbstractBasicDao<T,?> source, DaoEventType type, T vo, T originalVo) {
        super(source);
        this.type = Objects.requireNonNull(type);
        this.vo = Objects.requireNonNull(vo);
        this.originalVo = Objects.requireNonNull(originalVo);
    }

    public DaoEventType getType() {
        return type;
    }

    public T getVo() {
        return vo;
    }

    public T getOriginalVo() {
        return originalVo;
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
        return "DaoEvent [type=" + type + ", vo=" + vo + ", originalVo=" + originalVo + "]";
    }

}
