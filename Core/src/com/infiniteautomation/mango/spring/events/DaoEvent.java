/*
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.events;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

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
    private final String originalXid;
    private final Set<?> updatedFields;

    /**
     * @param source
     */
    public DaoEvent(AbstractBasicDao<T> source, DaoEventType type, T vo, String originalXid) {
        super(source);
        this.type = Objects.requireNonNull(type);
        this.vo = Objects.requireNonNull(vo);
        this.originalXid = originalXid;
        this.updatedFields = Collections.emptySet();
    }

    public DaoEvent(AbstractBasicDao<T> source, DaoEventType type, T vo, String originalXid, Set<?> updatedFields) {
        super(source);
        this.type = Objects.requireNonNull(type);
        this.vo = Objects.requireNonNull(vo);
        this.originalXid = originalXid;
        this.updatedFields = Objects.requireNonNull(updatedFields);
    }

    public DaoEventType getType() {
        return type;
    }

    public T getVo() {
        return vo;
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
        return ResolvableType.forClassWithGenerics(this.getClass(), ResolvableType.forClass(this.vo.getClass()));
    }

    public Set<?> getUpdatedFields() {
        return updatedFields;
    }

    @Override
    public String toString() {
        return "DaoEvent [type=" + type + ", vo=" + vo + ", originalXid=" + originalXid + ", updatedFields=" + updatedFields + "]";
    }

}
