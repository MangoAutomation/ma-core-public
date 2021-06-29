/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.spring;

import org.springframework.context.ApplicationEvent;

import com.serotonin.m2m2.LifecycleState;

/**
 * @author Jared Wiltshire
 */
public class LifecyleEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;
    private final LifecycleState lifecycleState;

    public LifecyleEvent(Object source, LifecycleState lifecycleState) {
        super(source);
        this.lifecycleState = lifecycleState;
    }

    public LifecycleState getLifecycleState() {
        return lifecycleState;
    }

}
