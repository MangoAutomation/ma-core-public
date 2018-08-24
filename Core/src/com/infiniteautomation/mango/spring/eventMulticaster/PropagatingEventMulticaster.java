/*
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.eventMulticaster;

import java.util.concurrent.ForkJoinPool;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.ResolvableType;

/**
 * A spring event multicaster that can propagate events to other multicasters. Typically used to propagate events from the
 * runtime context (e.g. DAO events) to the spring web context (e.g. to notify web sockets)
 *
 * @author Jared Wiltshire
 */

public class PropagatingEventMulticaster extends SimpleApplicationEventMulticaster {

    private final ApplicationContext context;
    private final EventMulticasterRegistry registry;

    public PropagatingEventMulticaster(ApplicationContext context, EventMulticasterRegistry registry) {
        super();
        this.setTaskExecutor(ForkJoinPool.commonPool());
        this.registry = registry;
        this.context = context;
    }

    @PostConstruct
    protected void init() {
        this.registry.register(this);
    }

    @PreDestroy
    protected void destroy() {
        this.registry.unregister(this);
    }

    @Override
    public void multicastEvent(ApplicationEvent event, ResolvableType eventType) {
        if (event instanceof ApplicationContextEvent) {
            this.doMulticastEvent(event, eventType);
            return;
        }

        // the multicast only happens from the root context, i.e. when the context has no parent
        if (this.context.getParent() == null) {
            for (ApplicationEventMulticaster registeredMulticaster : this.registry.getMulticasters()) {
                if (registeredMulticaster instanceof PropagatingEventMulticaster) {
                    ((PropagatingEventMulticaster) registeredMulticaster).doMulticastEvent(event, eventType);
                } else {
                    registeredMulticaster.multicastEvent(event, eventType);
                }
            }
        }
    }

    private void doMulticastEvent(ApplicationEvent event, ResolvableType eventType) {
        super.multicastEvent(event, eventType);
    }
}
