/*
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring;

import org.springframework.context.ApplicationEvent;

/**
 * @author Jared Wiltshire
 */
public class LifecyleEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;
    private final int lifecycleState;

    public LifecyleEvent(Object source, int lifecycleState) {
        super(source);
        this.lifecycleState = lifecycleState;
    }

    public int getLifecycleState() {
        return lifecycleState;
    }

}
